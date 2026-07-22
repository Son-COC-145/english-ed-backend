package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.CefrLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpeakingScenarioRequest {
    @NotBlank(message = "Không được để trống tiêu đề tiếng Việt")
    private String titleVi;
    
    @NotBlank(message = "Không được để trống tiêu đề tiếng Anh")
    private String titleEn;
    
    @NotBlank(message = "Không được để trống mô tả ngữ cảnh")
    private String contextDescription;
    
    @NotBlank(message = "Không được để trống tên nhân vật AI")
    private String aiRoleName;
    
    private String aiRoleAvatarUrl;
    
    @NotBlank(message = "Không được để trống prompt hệ thống cho AI")
    private String aiSystemPrompt;
    
    @NotBlank(message = "Không được để trống mô tả mục tiêu")
    private String goalDescription;
    
    private String hintPhrasesJson;
    
    @NotNull(message = "Không được để trống độ khó CEFR")
    private CefrLevel cefrLevel;
    
    private Short topicId;
    
    private Boolean isActive;
}
