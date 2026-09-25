package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminQuestionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private CefrLevel cefrLevel;
    private Skill skill;
    private QuestionType questionType;
    private String contentJson;
    private String correctAnswer;
    private Integer timeoutSeconds;
    private BigDecimal difficultyIndex;
    private String placementAudioUrl;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
