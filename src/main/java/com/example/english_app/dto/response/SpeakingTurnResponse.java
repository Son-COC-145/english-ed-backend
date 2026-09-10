package com.example.english_app.dto.response;

import java.util.List;

import com.example.english_app.entity.enums.SpeakerRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingTurnResponse {

    private Long id;
    private Integer turnIndex;
    private SpeakerRole speaker;
    private String status;
    private String evaluationStatus;
    private String errorCode;
    private String transcriptText;
    private String audioUrl;
    private Object audioMetrics;
    private List<GrammarCorrection> grammarErrors;
    private List<VocabularySuggestion> vocabularySuggestions;
    private LocalDateTime createdAt;
}
