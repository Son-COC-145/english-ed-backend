package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response tra ve cho Mobile khi nop audio phat am trong Placement Test.
 * Giai quyet van de P0 (Section 3.5 trong de xuat FE): Tra ve ca ket qua cham audio
 * LAN tien trinh cau tiep theo (nextQuestion hoac completion) trong cung 1 response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementPronunciationAnswerResponse {

    private Long sessionId;
    private Long submittedQuestionId;
    private String sessionStatus; // "IN_PROGRESS" | "COMPLETED"

    /**
     * Ket qua danh gia phat am chi tiet cua tu vua doc
     */
    private PronunciationScoreResult pronunciationResult;

    /**
     * Cau hoi tiep theo trong bai test
     */
    private PlacementQuestionResponse nextQuestion;

    private boolean isTestCompleted;
    private PlacementResultResponse placementResult;
}
