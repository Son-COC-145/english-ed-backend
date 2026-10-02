package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GoalSurveyRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsStructuredSurveyWithinSupportedLimits() {
        GoalSurveyRequest request = validRequest();

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMissingGoalAndEmptyFocusSkills() {
        GoalSurveyRequest request = validRequest();
        request.setLearningGoal(null);
        request.setFocusSkills(List.of());

        Set<ConstraintViolation<GoalSurveyRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("learningGoal", "focusSkills");
    }

    @Test
    void rejectsMoreThanThreeFocusSkills() {
        GoalSurveyRequest request = validRequest();
        request.setFocusSkills(List.of(
                LearnerSkill.SPEAKING,
                LearnerSkill.PRONUNCIATION,
                LearnerSkill.LISTENING,
                LearnerSkill.READING));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("focusSkills");
    }

    @Test
    void rejectsDailyStudyMinutesOutsideFiveToOneHundredTwenty() {
        GoalSurveyRequest request = validRequest();
        request.setDailyStudyMinutes(4);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("dailyStudyMinutes");

        request.setDailyStudyMinutes(121);
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("dailyStudyMinutes");
    }

    private GoalSurveyRequest validRequest() {
        return new GoalSurveyRequest(
                LearningGoal.COMMUNICATION,
                null,
                List.of(LearnerSkill.SPEAKING, LearnerSkill.PRONUNCIATION),
                30,
                "ONLINE",
                "BEGINNER");
    }
}
