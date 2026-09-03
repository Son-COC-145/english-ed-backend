package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {

    private boolean goalSurveyCompleted;
    private boolean placementTestCompleted;
    private boolean settingsCompleted;
    private boolean onboardingCompleted;

    /**
     * Bước tiếp theo cần thực hiện:
     * GOAL_SURVEY | PLACEMENT_TEST | SETTINGS | COMPLETE | COMPLETED
     */
    private String nextStep;

    private Integer stepNumber;
    private Integer totalSteps;
    private String userName;

    /**
     * Trạng thái chi tiết của Placement Test:
     * NOT_STARTED | IN_PROGRESS | COMPLETED | SKIPPED
     */
    private String placementTestStatus;

    /**
     * ID của session đang làm dở (nếu placementTestStatus == IN_PROGRESS).
     * Phục vụ Mobile resume an toàn mà không cần lưu ID vào local storage.
     */
    private Long activePlacementSessionId;

    private String placementCefrLevel;
    private Short dailyGoalXp;
    private boolean roadmapGenerated;
}
