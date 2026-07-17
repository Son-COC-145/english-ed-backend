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
     * GOAL_SURVEY | PLACEMENT_TEST | SETTINGS | COMPLETED
     */
    private String nextStep;

    private Integer stepNumber;
    private Integer totalSteps;
    private String userName;

    private String placementCefrLevel;
    private Short dailyGoalXp;
}
