package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response câu hỏi Placement Test.
 *
 * <p>Khi bài thi hoàn tất (đủ số câu hoặc confidence đạt ngưỡng),
 * {@code isTestCompleted=true} và {@code placementResult} sẽ được điền.
 * Frontend kiểm tra flag này để biết chuyển sang màn hình kết quả.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementQuestionResponse {

    private Long sessionId;
    private Long submittedQuestionId;

    /**
     * Trạng thái session: "IN_PROGRESS" hoặc "COMPLETED"
     */
    @Builder.Default
    private String sessionStatus = "IN_PROGRESS";

    /**
     * Thông tin câu hỏi tiếp theo (Flattened properties)
     */
    private Long questionId;
    private int questionIndex;
    private int totalQuestions;
    private String cefrLevel;
    private String skill;
    private String questionType;
    private Integer timeoutSeconds;
    private Map<String, Object> content;

    /**
     * Object lồng nextQuestion (Nested properties) theo đúng hợp đồng đề xuất của FE:
     * { "submittedQuestionId": 6, "sessionStatus": "IN_PROGRESS", "nextQuestion": { ... } }
     */
    private Map<String, Object> nextQuestion;

    // ─── Completion signal ───────────────────────────────────────────────────────────
    /**
     * true khi bài thi vừa hoàn tất trong lần submit này.
     * Key JSON bắt buộc là "isTestCompleted" theo hợp đồng FE.
     */
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonProperty("isTestCompleted")
    private boolean isTestCompleted = false;

    /** Điền sẵn kết quả khi isTestCompleted=true, null nếu còn tiếp tục làm. */
    private PlacementResultResponse placementResult;

    // ─── Feedback for previous answer (Gamification) ───────────────────────────
    private Boolean previousAnswerCorrect;
    private String previousCorrectAnswer;
    private String previousExplanation;
}

