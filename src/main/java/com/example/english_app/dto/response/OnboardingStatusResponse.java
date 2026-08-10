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
     * Bước tiếp theo cần thực hiện.
     * <p>Giá trị hợp lệ (theo thứ tự flow):
     * {@code GOAL_SURVEY} → {@code PLACEMENT_TEST} → {@code ROADMAP_VIEW} → {@code SETTINGS} → {@code COMPLETED}
     */
    private String nextStep;

    private Integer stepNumber;
    private Integer totalSteps;
    private String userName;

    private String placementCefrLevel;
    private Short dailyGoalXp;
    private boolean roadmapGenerated;
}
