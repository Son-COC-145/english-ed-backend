package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AssignmentCandidateSource implements TodayPlanCandidateSource {

    private final AssignmentRepository assignmentRepository;
    private final AdaptiveRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        List<Assignment> assignments = assignmentRepository
                .findNotSubmittedForStudent(context.studentId(), PageRequest.of(0, 30)).getContent();
        List<TodayPlanCandidate> candidates = new ArrayList<>();
        for (Assignment assignment : assignments) {
            if (assignment.getDeadlineAt() == null
                    || assignment.getDeadlineAt().isAfter(
                            context.now().plusDays(properties.getAssignmentHorizonDays()))) {
                continue;
            }
            candidates.add(candidate(context, assignment));
        }
        return candidates;
    }

    private TodayPlanCandidate candidate(TodayPlanContext context, Assignment assignment) {
        long hours = Duration.between(context.now(), assignment.getDeadlineAt()).toHours();
        RecommendationPriority priority = hours < 0
                ? RecommendationPriority.P0
                : hours < 24 ? RecommendationPriority.P1 : null;
        RecommendationReasonCode reason = priority == RecommendationPriority.P0
                ? RecommendationReasonCode.ASSIGNMENT_OVERDUE
                : RecommendationReasonCode.ASSIGNMENT_DUE;
        LearnerSkill skill = skill(assignment.getModuleType());
        int estimate = switch (assignment.getModuleType()) {
            case SPEAKING -> 8;
            case PRONUNCIATION -> 3;
            case VOCABULARY -> 5;
        };
        var reasonParams = objectMapper.createObjectNode();
        if (priority == RecommendationPriority.P0) {
            reasonParams.put("daysOverdue", Math.max(1,
                    Duration.between(assignment.getDeadlineAt(), context.now()).toDays()));
        } else {
            reasonParams.put("hoursRemaining", Math.max(0, hours));
        }
        return new TodayPlanCandidate(
                TodayPlanItemType.ASSIGNMENT,
                assignment.getTitle(),
                skill,
                objectMapper.createObjectNode()
                        .put("courseId", assignment.getCourse().getId())
                        .put("assignmentId", assignment.getId()),
                objectMapper.createObjectNode()
                        .put("route", "ASSIGNMENT")
                        .put("courseId", assignment.getCourse().getId())
                        .put("assignmentId", assignment.getId()),
                reason,
                reasonParams,
                priority,
                estimate,
                0,
                1,
                objectMapper.createObjectNode().put("assignmentId", assignment.getId()),
                priority == RecommendationPriority.P0 ? 1.0 : Math.max(0, 1 - hours / 72.0),
                CandidateSupport.weakness(context, skill),
                0,
                CandidateSupport.goalFocus(context, skill) ? 1 : 0,
                1,
                assignment.getDeadlineAt());
    }

    private LearnerSkill skill(ModuleType type) {
        return switch (type) {
            case PRONUNCIATION -> LearnerSkill.PRONUNCIATION;
            case SPEAKING -> LearnerSkill.SPEAKING;
            case VOCABULARY -> LearnerSkill.VOCABULARY;
        };
    }
}
