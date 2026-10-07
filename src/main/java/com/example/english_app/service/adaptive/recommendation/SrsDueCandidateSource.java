package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SrsDueCandidateSource implements TodayPlanCandidateSource {

    private static final int MAX_WORDS = 20;

    private final StudentVocabularyProgressRepository progressRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        long dueCount = progressRepository.countDueReviews(context.studentId(), context.now());
        if (dueCount == 0) return List.of();
        List<StudentVocabularyProgress> due = progressRepository.findDueReviews(
                context.studentId(), context.now(), null, null, PageRequest.of(0, MAX_WORDS)).getContent();
        if (due.isEmpty()) return List.of();

        ArrayNode ids = objectMapper.createArrayNode();
        due.forEach(progress -> ids.add(progress.getVocabulary().getId()));
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.set("vocabularyIds", ids);

        int estimatedMinutes = Math.max(1, (int) Math.ceil(Math.min(dueCount, MAX_WORDS) / 6.0));
        return List.of(new TodayPlanCandidate(
                TodayPlanItemType.VOCABULARY_REVIEW,
                "Ôn từ đến hạn",
                LearnerSkill.VOCABULARY,
                objectMapper.createObjectNode().put("mode", "DUE"),
                objectMapper.createObjectNode().put("route", "VOCABULARY_REVIEW").put("mode", "DUE"),
                RecommendationReasonCode.SRS_DUE,
                objectMapper.createObjectNode().put("dueCount", dueCount),
                null,
                estimatedMinutes,
                0,
                due.size(),
                snapshot,
                Math.min(1.0, dueCount / 20.0),
                CandidateSupport.weakness(context, LearnerSkill.VOCABULARY),
                0,
                CandidateSupport.goalFocus(context, LearnerSkill.VOCABULARY) ? 1 : 0,
                1,
                null));
    }
}
