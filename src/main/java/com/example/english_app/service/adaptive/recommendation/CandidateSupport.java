package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.entity.adaptive.LearnerSkillState;
import com.example.english_app.entity.enums.LearnerSkill;

final class CandidateSupport {

    private CandidateSupport() {
    }

    static double weakness(TodayPlanContext context, LearnerSkill skill) {
        return context.skillStates().stream()
                .filter(state -> state.getSkill() == skill)
                .map(LearnerSkillState::getMastery)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(value -> Math.max(0.0, 1.0 - value.doubleValue() / 100.0))
                .orElse(0.5);
    }

    static boolean goalFocus(TodayPlanContext context, LearnerSkill skill) {
        return context.survey().focusSkills().contains(skill);
    }
}
