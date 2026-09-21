package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.*;
import com.example.english_app.service.integration.TtsGenerationService;
import com.example.english_app.service.integration.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.AdditionalMatchers.aryEq;

class SpeakingWorkerTest {
    final SpeakingStore store = mock(SpeakingStore.class);
    final SpeakingAiClient ai = mock(SpeakingAiClient.class);
    final TtsGenerationService tts = mock(TtsGenerationService.class);
    final CloudinaryService cloudinary = mock(CloudinaryService.class);
    final SpeakingJson json = new SpeakingJson(new ObjectMapper());
    final SpeakingWorker worker = new SpeakingWorker(store, json, ai, new AudioMetricsService(), tts, cloudinary);

    SpeakingStore.Job job(String kind) {
        when(store.session(1L)).thenReturn(SpeakingSession.builder().scenarioSnapshotJson("{\"cefr\":\"A2\"}").build());
        return new SpeakingStore.Job(1, 1, 2L, kind, 1, "token");
    }
    @AfterEach void close() { worker.close(); }

    @Test void ttsRetryReusesDurableTextWithoutLlm() throws Exception {
        var job = job("RESPONSE");
        when(store.turn(2L)).thenReturn(SpeakingTurn.builder().transcriptText("Hello").responseTextReady(true).build());
        when(tts.generateAudioStream("Hello", null)).thenReturn(new byte[]{1});
        when(cloudinary.uploadFile(any(), eq("video"), anyString())).thenReturn("https://cdn.test/audio.mp3");
        worker.process(job);
        verifyNoInteractions(ai);
        verify(store).responseDone(eq(job), eq("Hello"), aryEq(new byte[]{1}), eq("https://cdn.test/audio.mp3"));
    }

    @Test void lostLeaseDoesNotStartTts() throws Exception {
        var job = job("RESPONSE");
        when(store.turn(2L)).thenReturn(SpeakingTurn.builder().turnIndex(1).transcriptText("").build());
        when(ai.reply(any(), anyList(), any())).thenReturn("Hello");
        when(store.textReady(job, "Hello")).thenReturn(false);
        worker.process(job);
        verifyNoInteractions(tts);
        verify(store, never()).responseDone(any(), anyString(), any());
    }

    @Test void providerFailureIsNotSuccessfulDialogue() throws Exception {
        var job = job("RESPONSE");
        when(store.turn(2L)).thenReturn(SpeakingTurn.builder().turnIndex(1).transcriptText("").build());
        when(ai.reply(any(), anyList(), any())).thenThrow(new java.io.IOException("unavailable"));
        assertThatThrownBy(() -> worker.process(job)).isInstanceOf(java.io.IOException.class);
        verify(store, never()).responseDone(any(), anyString(), any());
        verifyNoInteractions(tts);
    }

    @Test void evaluationWaitsForInFlightDialogue() throws Exception {
        var job = job("SESSION_EVALUATION");
        when(store.history(1L)).thenReturn(List.of(SpeakingTurn.builder().speaker(SpeakerRole.STUDENT).status("PENDING").build()));
        worker.process(job);
        verify(store).defer(job);
        verifyNoInteractions(ai);
    }

    @Test void failedTurnMakesReportRetryableWithoutInventedGrade() throws Exception {
        var job = job("SESSION_EVALUATION");
        when(store.history(1L)).thenReturn(List.of(SpeakingTurn.builder().speaker(SpeakerRole.AI).status("FAILED").build()));
        worker.process(job);
        verify(store).reportBlocked(job);
        verifyNoInteractions(ai);
    }

    @Test void missingDurationNeverProducesFluency100OrIntonationGrade() throws Exception {
        var job = job("SESSION_EVALUATION");
        when(store.history(1L)).thenReturn(List.of(SpeakingTurn.builder().speaker(SpeakerRole.STUDENT)
                .status("COMPLETED").evaluationStatus("COMPLETED").audioMetricsJson("{\"wordCount\":4}").build()));
        when(ai.evaluateGoals(any(), anyList())).thenReturn(json.read("{\"task_completion_score\":100}"));
        worker.process(job);
        verify(store).reportDone(eq(job), argThat(r ->
                r.path("fluency").path("scoreStatus").asText().equals("INSUFFICIENT_DATA")
                        && r.path("intonationStatus").asText().equals("NOT_ASSESSED")), isNull(), isNull());
    }
}
