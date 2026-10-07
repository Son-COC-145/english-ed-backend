package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.classroom.SyllabusItemRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ClassSyllabusCandidateSource implements TodayPlanCandidateSource {

    private static final int WORD_LIMIT = 10;

    private final CourseStudentRepository courseStudentRepository;
    private final SyllabusItemRepository syllabusItemRepository;
    private final VocabularyRepository vocabularyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        List<TodayPlanCandidate> candidates = new ArrayList<>();
        for (Course course : courseStudentRepository.findActiveCoursesByStudentId(context.studentId())) {
            if (course.getStartDate() == null) continue;
            long days = ChronoUnit.DAYS.between(course.getStartDate(), context.planDate());
            short week = (short) (Math.max(0, days) / 7 + 1);
            List<Long> topicIds = syllabusItemRepository.findTopicIdsByCoursesAndWeeks(
                    List.of(course.getId()), List.of(week));
            if (topicIds.isEmpty()) continue;
            List<Vocabulary> words = vocabularyRepository.findDeterministicNewVocabularies(
                    topicIds, context.studentId(), PageRequest.of(0, WORD_LIMIT));
            if (words.isEmpty()) continue;
            Vocabulary first = words.getFirst();
            ArrayNode ids = objectMapper.createArrayNode();
            words.forEach(word -> ids.add(word.getId()));
            var snapshot = objectMapper.createObjectNode();
            snapshot.set("vocabularyIds", ids);
            candidates.add(new TodayPlanCandidate(
                    TodayPlanItemType.VOCABULARY_TOPIC,
                    "Từ mới theo lớp: " + course.getName(),
                    LearnerSkill.VOCABULARY,
                    objectMapper.createObjectNode().put("topicId", first.getTopic().getId()),
                    objectMapper.createObjectNode().put("route", "VOCABULARY_TOPIC")
                            .put("topicId", first.getTopic().getId()),
                    RecommendationReasonCode.CLASS_SYLLABUS,
                    objectMapper.createObjectNode().put("courseId", course.getId()),
                    null,
                    5,
                    0,
                    words.size(),
                    snapshot,
                    0,
                    CandidateSupport.weakness(context, LearnerSkill.VOCABULARY),
                    0,
                    CandidateSupport.goalFocus(context, LearnerSkill.VOCABULARY) ? 1 : 0,
                    1,
                    null));
        }
        return candidates;
    }
}
