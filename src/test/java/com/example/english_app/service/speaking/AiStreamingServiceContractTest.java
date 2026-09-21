package com.example.english_app.service.speaking;

import com.example.english_app.controller.SpeakingSessionController;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingTurnResponse;
import com.example.english_app.entity.enums.SpeakerRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AiStreamingServiceContractTest {
    final SpeakingSessionService sessions = mock(SpeakingSessionService.class);
    final SpeakingAccess access = mock(SpeakingAccess.class);
    final AiStreamingService streaming = new AiStreamingService(sessions, access, new ObjectMapper());
    MockMvc mvc;

    @BeforeEach void setup() {
        when(access.userId()).thenReturn(7L);
        mvc = MockMvcBuilders.standaloneSetup(new SpeakingSessionController(sessions, streaming)).build();
    }

    @AfterEach void shutdown() { streaming.close(); }

    SpeakingSessionService.StreamingSnapshot snapshot(String status, String evaluation, boolean finalText) {
        var turn = SpeakingTurnResponse.builder().id(103L).turnIndex(3).speaker(SpeakerRole.AI)
                .transcriptText("Same finalized text").status(status).evaluationStatus(evaluation)
                .audioStatus("COMPLETED".equals(status) ? "READY" : "PENDING")
                .audioUrl("COMPLETED".equals(status) ? "/api/v1/speaking-session/42/turns/103/audio" : null).build();
        var report = SessionEvaluationResponse.builder().sessionId(42L).status("ONGOING").turns(List.of(turn)).build();
        return new SpeakingSessionService.StreamingSnapshot(report, finalText ? Set.of(103L) : Set.of());
    }

    String readStream() throws Exception {
        var result = mvc.perform(get("/api/v1/speaking-session/42/stream-response")).andReturn();
        result.getAsyncResult(5000);
        return mvc.perform(asyncDispatch(result)).andReturn().getResponse().getContentAsString();
    }

    @Test void finalizedTextBeforeAudioAndEvaluationOnlyChangeAreDelivered() throws Exception {
        when(sessions.streamingSnapshot(42L, 7L)).thenReturn(
                snapshot("PENDING", "PENDING", false),
                snapshot("PENDING", "COMPLETED", true),
                snapshot("COMPLETED", "COMPLETED", true));
        String body = readStream();
        assertThat(body).contains("event:session", "event:text-final", "event:audio", "event:done");
        assertThat(body.split("event:text\n", -1)).hasSize(4);
        assertThat(body.split("event:text-final", -1)).hasSize(2);
        assertThat(body.indexOf("event:text-final")).isLessThan(body.indexOf("event:audio"));
        assertThat(body).doesNotContain("textReady");
    }

    @Test void reconnectRecoversCompletedGreetingWithoutSchedulingMutation() throws Exception {
        when(sessions.streamingSnapshot(42L, 7L)).thenReturn(snapshot("COMPLETED", "NOT_APPLICABLE", true));
        String first = readStream();
        String second = readStream();
        assertThat(first).contains("event:text-final", "event:audio", "event:done");
        assertThat(second).isEqualTo(first);
        verify(sessions, never()).retry(anyLong());
        verify(sessions, never()).retryTurn(anyLong(), anyLong());
    }

    @Test void failedTurnEmitsCorrelatedError() throws Exception {
        when(sessions.streamingSnapshot(42L, 7L)).thenReturn(snapshot("FAILED", "NOT_APPLICABLE", false));
        assertThat(readStream()).contains("event:error", "\"turnId\":103", "\"sessionId\":42", "\"errorCode\":");
    }
}
