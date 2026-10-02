package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingReportSummaryResponse {

    private Long sessionId;
    private Short scenarioId;
    private String scenarioTitleVi;
    private String scenarioTitleEn;
    private CefrLevel cefrLevel;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Short hintUsedCount;
    private Short taskCompletionScore;
    private Short fluencyScore;
    private Short intonationScore;
    private Short xpEarned;
}
