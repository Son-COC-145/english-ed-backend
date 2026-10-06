package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.LearnerProfile;
import com.example.english_app.entity.adaptive.LearnerSkillState;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.service.onboarding.GoalSurveyParser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TodayPlanContext(
        Long studentId,
        LocalDate planDate,
        LocalDateTime now,
        StudentOnboarding onboarding,
        GoalSurveyParser.ParsedGoalSurvey survey,
        LearnerProfile profile,
        List<LearnerSkillState> skillStates,
        RoadmapResponse roadmap,
        RoadmapProgressResponse roadmapProgress) {
}
