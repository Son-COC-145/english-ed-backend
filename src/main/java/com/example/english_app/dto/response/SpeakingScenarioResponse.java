package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.CefrLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpeakingScenarioResponse {
    private Short id;
    private String titleVi;
    private String titleEn;
    private String contextDescription;
    private String aiRoleName;
    private String aiRoleAvatarUrl;
    private String aiSystemPrompt;
    private String goalDescription;
    private Object hintPhrases; 
    private CefrLevel cefrLevel;
    private Short topicId;
    private Boolean isActive;
}
