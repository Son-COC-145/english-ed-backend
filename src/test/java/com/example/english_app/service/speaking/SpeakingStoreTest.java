package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.*;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.speaking.*;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpeakingStoreTest {
    final SpeakingSessionRepository sessions = mock(SpeakingSessionRepository.class);
    final SpeakingScenarioRepository scenarios = mock(SpeakingScenarioRepository.class);
    final SpeakingTurnRepository turns = mock(SpeakingTurnRepository.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final SpeakingStore store = new SpeakingStore(sessions, scenarios, turns, mock(UserRepository.class), jdbc,
            new SpeakingJson(new ObjectMapper()));
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
        assertThatThrownBy(() -> store.owned(1L, 8L)).isInstanceOf(com.example.english_app.exception.AppException.class);
        assertThatThrownBy(() -> store.end(1L, 8L)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> store.submit(1L, 8L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(jdbc, turns);
    }

    @Test void inactiveScenarioCannotStart() {
        when(scenarios.findById((short) 1)).thenReturn(Optional.of(SpeakingScenario.builder().isActive(false).build()));
        assertThatThrownBy(() -> store.start(7L, (short) 1)).isInstanceOf(RuntimeException.class);
        verify(sessions, never()).saveAndFlush(any());
        verifyNoInteractions(jdbc);
    }

    @Test void endClosesInputWhileAcceptedTurnFinishesAndIsIdempotent() {
        when(turns.findBySessionIdOrderByTurnIndexAscIdAsc(1L)).thenReturn(List.of(
                SpeakingTurn.builder().speaker(SpeakerRole.STUDENT).status("PENDING").build()));
        store.end(1L, 7L);
        assertThat(session.getStatus()).isEqualTo("EVALUATING");
        store.end(1L, 7L);
        verify(jdbc, times(1)).update(anyString(), eq(1L), isNull(), eq("SESSION_EVALUATION"));
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test void completedEndNeverEnqueuesOrAwardsAgain() {
        session.setStatus("COMPLETED");
        assertThat(store.end(1L, 7L)).isSameAs(session);
        verifyNoInteractions(jdbc, turns);
    }

    @Test void duplicateKeyReturnsExistingEvenAfterEndButChangedPayloadConflicts() {
        session.setStatus("EVALUATING");
        var existing = SpeakingTurn.builder().id(3L).inputHash("same").build();
        when(turns.findBySessionIdAndRequestKey(1L, "request01")).thenReturn(Optional.of(existing));
        assertThat(store.submit(1L, 7L, "request01", "same", null, null, "Hi")).isSameAs(existing);
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "different", null, null, "Bye"))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(jdbc);
        verify(turns, never()).saveAndFlush(any());
    }

    @Test void pendingTurnPreventsHistoryOverwrite() {
        when(turns.findBySessionIdOrderByTurnIndexAscIdAsc(1L)).thenReturn(List.of(
                SpeakingTurn.builder().status("PENDING").build()));
        assertThatThrownBy(() -> store.submit(1L, 7L, "request01", "hash", null, null, "Hi"))
                .isInstanceOf(RuntimeException.class);
        verify(turns, never()).saveAndFlush(any());
    }
}
