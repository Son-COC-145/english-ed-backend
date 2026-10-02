package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.*;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.exception.AppException;
import com.example.english_app.repository.speaking.*;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpeakingStoreTest {
    final SpeakingSessionRepository sessions = mock(SpeakingSessionRepository.class);
    final SpeakingScenarioRepository scenarios = mock(SpeakingScenarioRepository.class);
    final SpeakingStartRequestRepository startRequests = mock(SpeakingStartRequestRepository.class);
    final SpeakingTurnRepository turns = mock(SpeakingTurnRepository.class);
    final SpeakingJobRepository jobs = mock(SpeakingJobRepository.class);
    final SpeakingRewardRepository rewards = mock(SpeakingRewardRepository.class);
    final org.springframework.context.ApplicationEventPublisher publisher = mock(org.springframework.context.ApplicationEventPublisher.class);
    final SpeakingStore store = new SpeakingStore(sessions, scenarios, startRequests, turns, mock(UserRepository.class), jobs, rewards,
            new SpeakingJson(new ObjectMapper()), publisher);
    SpeakingSession session;

    @BeforeEach void setup() {
        session = SpeakingSession.builder().id(1L).student(User.builder().id(7L).build()).build();
        when(sessions.lockById(1L)).thenReturn(Optional.of(session));
        when(sessions.findById(1L)).thenReturn(Optional.of(session));
    }

    @Test void reportOwnershipDoesNotTakeWriteLock() {
        assertThat(store.owned(1L, 7L)).isSameAs(session);
        verify(sessions, never()).lockById(anyLong());
    }

    @Test void otherStudentCannotReadOrMutate() {
        assertThatThrownBy(() -> store.owned(1L, 8L)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> store.end(1L, 8L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> store.submit(1L, 8L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(jobs, rewards, turns);
    }

    @Test void inactiveScenarioCannotStart() {
        when(scenarios.findById((short) 1)).thenReturn(Optional.of(SpeakingScenario.builder().isActive(false).build()));
        assertThatThrownBy(() -> store.start(7L, (short) 1, "startkey1")).isInstanceOf(RuntimeException.class);
        verify(sessions, never()).saveAndFlush(any());
        verifyNoInteractions(jobs, rewards);
    }

    @Test void endClosesInputWhileAcceptedTurnFinishesAndIsIdempotent() {
        when(turns.findBySessionIdOrderByTurnIndexAscIdAsc(1L)).thenReturn(List.of(
                SpeakingTurn.builder().speaker(SpeakerRole.STUDENT).status("PENDING").build()));
        store.end(1L, 7L);
        assertThat(session.getStatus()).isEqualTo("EVALUATING");
        store.end(1L, 7L);
        verify(jobs, times(1)).enqueue(1L, null, "SESSION_EVALUATION");
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test void completedEndNeverEnqueuesOrAwardsAgain() {
        session.setStatus("COMPLETED");
        assertThat(store.end(1L, 7L)).isSameAs(session);
        verifyNoInteractions(jobs, rewards, turns);
    }

    @Test void endWithoutStudentTurnCompletesWithoutCreatingReportJobOrReward() {
        when(turns.findBySessionIdOrderByTurnIndexAscIdAsc(1L)).thenReturn(List.of(
                SpeakingTurn.builder().speaker(SpeakerRole.AI).status("COMPLETED").build()));

        assertThat(store.end(1L, 7L).getStatus()).isEqualTo("CANCELLED");
        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getEvaluationJson()).isNull();
        verifyNoInteractions(jobs, rewards);
    }

    @Test void duplicateKeyReturnsExistingEvenAfterEndButChangedPayloadConflicts() {
        session.setStatus("EVALUATING");
        var existing = SpeakingTurn.builder().id(3L).inputHash("same").build();
        when(turns.findBySessionIdAndRequestKey(1L, "request01")).thenReturn(Optional.of(existing));
        assertThat(store.submit(1L, 7L, "request01", "same", null, null, "Hi")).isSameAs(existing);
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "different", null, null, "Bye"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(jobs, rewards);
        verify(turns, never()).saveAndFlush(any());
    }

    @Test void pendingTurnPreventsHistoryOverwrite() {
        when(turns.findBySessionIdOrderByTurnIndexAscIdAsc(1L)).thenReturn(List.of(
                SpeakingTurn.builder().status("PENDING").build()));
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
        verify(turns, never()).saveAndFlush(any());
    }

    @Test void noSpeechStopsAutomaticRetryBeforeBudgetIsExhausted() {
        var job = new SpeakingStore.Job(11L, 1L, 3L, "INPUT", 1, "lease-token");
        var turn = SpeakingTurn.builder().id(3L).speaker(SpeakerRole.STUDENT).build();
        when(jobs.findCurrentForUpdate(11L, "lease-token")).thenReturn(Optional.of(11L));
        when(jobs.maxAttempts(11L)).thenReturn(3);
        when(turns.findById(3L)).thenReturn(Optional.of(turn));
        store.failed(job, ErrorCode.SPEAKING_AUDIO_NO_SPEECH.toException());
        assertThat(turn.getStatus()).isEqualTo("FAILED");
        assertThat(turn.getErrorCode()).isEqualTo("SPEAKING_AUDIO_NO_SPEECH");
        verify(jobs).updateFailure(eq(11L), eq("FAILED"), eq(true), eq("SPEAKING_AUDIO_NO_SPEECH"), anyString(), anyInt());
        verify(jobs).finishAttempt(eq(11L), eq("lease-token"), eq("FAILED"), eq("SPEAKING_AUDIO_NO_SPEECH"), anyString(), eq(false));
    }

    @Test void completingReportDoesNotAddXpWhenRewardAlreadyExists() {
        var job = new SpeakingStore.Job(11L, 1L, null, "SESSION_EVALUATION", 1, "lease-token");
        session.setStatus("EVALUATING");
        when(jobs.findCurrentForUpdate(11L, "lease-token")).thenReturn(Optional.of(11L));
        when(rewards.insertOnce(1L, 7L, (short) 30)).thenReturn(0);
        store.reportDone(job, new ObjectMapper().createObjectNode().put("task_completion_score", 100), null, null);
        assertThat(session.getStatus()).isEqualTo("COMPLETED");
        assertThat(session.getTaskCompletionScore()).isEqualTo((short) 100);
        assertThat(session.getXpEarned()).isEqualTo((short) 30);
        verify(rewards, never()).incrementXp(anyLong(), anyShort());
        verify(jobs).complete(11L);
    }
}
