package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.SpeakerRole;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SpeakingTurnResponse {
    private Long id;
    private Short turnIndex;
    private SpeakerRole speaker;
    private String transcriptText;
    private String audioUrl;
    private Object grammarErrors;
    private Object vocabularySuggestions;
    private LocalDateTime createdAt;
}
