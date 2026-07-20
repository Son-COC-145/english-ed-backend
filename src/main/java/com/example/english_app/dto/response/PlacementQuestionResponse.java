package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementQuestionResponse {

    private Long sessionId;
    private Long questionId;
    private int questionIndex;
    private int totalQuestions;

    private String cefrLevel;
    private String skill;
    private String questionType;
    private Integer timeoutSeconds;

    private Map<String, Object> content;
}
