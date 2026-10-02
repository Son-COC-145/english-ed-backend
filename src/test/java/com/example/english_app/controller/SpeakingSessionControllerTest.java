package com.example.english_app.controller;

import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingReportSummaryResponse;
import com.example.english_app.service.speaking.AiStreamingService;
import com.example.english_app.service.speaking.SpeakingSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpeakingSessionControllerTest {

    private final SpeakingSessionService sessions = mock(SpeakingSessionService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new SpeakingSessionController(sessions, mock(AiStreamingService.class)))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void endingSessionWithoutStudentTurnReturnsTerminalCancelledResponse() throws Exception {
        when(sessions.endSession(42L)).thenReturn(SessionEvaluationResponse.builder()
                .sessionId(42L)
                .status("CANCELLED")
                .build());

        mvc.perform(post("/api/v1/speaking-session/42/end"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(42))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void reportHistoryEndpointReturnsPagedSummaries() throws Exception {
        var summary = SpeakingReportSummaryResponse.builder()
                .sessionId(41L)
                .scenarioTitleVi("Gọi món")
                .taskCompletionScore((short) 85)
                .build();
        when(sessions.reports(any())).thenReturn(PageResponse.<SpeakingReportSummaryResponse>builder()
                .currentPage(0)
                .pageSize(20)
                .totalPages(1)
                .totalElements(1)
                .content(List.of(summary))
                .build());

        mvc.perform(get("/api/v1/speaking-session/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sessionId").value(41))
                .andExpect(jsonPath("$.data.content[0].scenarioTitleVi").value("Gọi món"))
                .andExpect(jsonPath("$.data.content[0].taskCompletionScore").value(85));
    }
}
