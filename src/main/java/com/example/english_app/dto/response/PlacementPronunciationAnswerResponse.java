package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response trả về cho Mobile khi nộp audio phát âm trong Placement Test.
 * Giải quyết vấn đề P0 (Section 3.5 trong đề xuất FE): Trả về cả kết quả chấm audio
 * LẪN tiến trình câu tiếp theo (nextQuestion hoặc completion) trong cùng 1 response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementPronunciationAnswerResponse {

    /**
     * ID của phiên làm bài kiểm tra đầu vào.
     */
    private Long sessionId;

    /**
     * ID của câu hỏi phát âm vừa được nộp.
     */
    private Long submittedQuestionId;

    /**
     * Trạng thái phiên kiểm tra: "IN_PROGRESS" | "COMPLETED".
     */
    private String sessionStatus;

    /**
     * Kết quả đánh giá phát âm chi tiết của từ vừa đọc.
     */
    private PronunciationScoreResult pronunciationResult;

    /**
     * Câu hỏi tiếp theo trong bài test (nếu bài test chưa kết thúc).
     */
    private PlacementQuestionResponse nextQuestion;

    /**
     * Đánh dấu bài test đã hoàn thành hay chưa.
     */
    private boolean isTestCompleted;

    /**
     * Kết quả tổng kết bài kiểm tra đầu vào (khi isTestCompleted == true).
     */
    private PlacementResultResponse placementResult;
}
