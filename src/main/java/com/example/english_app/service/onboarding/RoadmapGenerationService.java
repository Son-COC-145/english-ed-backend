package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.TopicCategory;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service độc lập chịu trách nhiệm sinh lộ trình học tập (Roadmap) dựa trên
 * mục tiêu và trình độ của học viên.
 *
 * <p>Sử dụng transaction REQUIRES_NEW để đảm bảo nếu quá trình sinh lộ trình lỗi,
 * nó sẽ KHÔNG làm rollback kết quả làm bài Placement Test của user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoadmapGenerationService {

    private final GoalSurveyParser goalSurveyParser;
    private final TopicRepository topicRepository;
    private final VocabularyRepository vocabularyRepository;
    private final SpeakingScenarioRepository speakingScenarioRepository;
    private final OnboardingRepository onboardingRepository;
    private final ObjectMapper objectMapper;

    /**
     * Entry point: Tạo và lưu lộ trình vào DB.
     * Transaction độc lập để không ảnh hưởng đến luồng chính.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RoadmapResponse generateAndPersist(Long userId, CefrLevel level, String goalSurveyJson) {
        log.info("Starting roadmap generation for user {}, CEFR={}, goals={}", userId, level, goalSurveyJson);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            // 1. Phân tích mục tiêu
            List<TopicCategory> categories = goalSurveyParser.extractCategories(goalSurveyJson);
            List<String> focusSkills = goalSurveyParser.extractFocusSkills(goalSurveyJson);
            
            // 2. Build roadmap logic
            RoadmapResponse roadmap = buildRoadmap(level, categories, focusSkills);

            // 3. Serialize và lưu vào StudentOnboarding
            Optional<StudentOnboarding> optOnboarding = onboardingRepository.findByStudentId(userId);
            if (optOnboarding.isPresent()) {
                StudentOnboarding onboarding = optOnboarding.get();
                String roadmapJson = objectMapper.writeValueAsString(roadmap);
                onboarding.setRoadmapJson(roadmapJson);
                onboardingRepository.save(onboarding);
                log.info("Persisted roadmap_json for user {}", userId);
            } else {
                log.warn("Cannot persist roadmap: Onboarding record not found for user {}", userId);
            }

            stopWatch.stop();
            if (stopWatch.getTotalTimeMillis() > 500) {
                log.warn("Performance warning: Roadmap generation took {} ms", stopWatch.getTotalTimeMillis());
            } else {
                log.debug("Roadmap generated in {} ms", stopWatch.getTotalTimeMillis());
            }

            return roadmap;

        } catch (Exception e) {
            log.error("Failed to generate and persist roadmap for user {}", userId, e);
            throw new RuntimeException("Roadmap generation failed", e);
        }
    }

    /**
     * Logic thuần tuý để lắp ráp lộ trình.
     */
    private RoadmapResponse buildRoadmap(CefrLevel level, List<TopicCategory> categories, List<String> focusSkills) {
        
        List<String> categoryNames = categories.stream().map(Enum::name).collect(Collectors.toList());
        
        // 1. Lấy Topics (ưu tiên đúng level và đúng category)
        List<Topic> topics = selectTopics(level, categoryNames);
        
        // 2. Lấy Speaking Scenarios (tuỳ vào focusSkills)
        boolean hasSpeaking = focusSkills != null && focusSkills.stream().anyMatch(s -> s.equalsIgnoreCase("Giao tiếp"));
        
        List<RoadmapMilestone> milestones = assembleMilestones(level, topics, hasSpeaking);

        return RoadmapResponse.builder()
                .cefrLevel(level.name())
                .totalWeeks(milestones.size())
                .milestones(milestones)
                .build();
    }

    private List<Topic> selectTopics(CefrLevel level, List<String> categories) {
        List<Topic> primaryTopics = topicRepository.findForRoadmap(level, categories, PageRequest.of(0, 10));
        
        // Fallback nếu không có topic nào thoả mãn
        if (primaryTopics.isEmpty()) {
            primaryTopics = topicRepository.findForRoadmap(level, null, PageRequest.of(0, 5));
        }
        // Fallback lần 2 nếu vẫn rỗng (Database quá ít dữ liệu)
        if (primaryTopics.isEmpty()) {
            primaryTopics = topicRepository.findForRoadmap(null, null, PageRequest.of(0, 5));
        }
        return primaryTopics;
    }

    private List<RoadmapMilestone> assembleMilestones(CefrLevel level, List<Topic> topics, boolean includeSpeaking) {
        List<RoadmapMilestone> milestones = new ArrayList<>();
        
        int week = 1;
        for (Topic topic : topics) {
            List<RoadmapModule> modules = new ArrayList<>();
            
            // 1. Vocabulary Module
            int vocabCount = (int) vocabularyRepository.countByTopicId(topic.getId());
            if (vocabCount > 0) {
                modules.add(RoadmapModule.builder()
                        .type("VOCABULARY")
                        .title("Từ vựng: " + topic.getNameVi())
                        .topicId(topic.getId())
                        .itemCount(vocabCount)
                        .cefrLevel(topic.getCefrLevel() != null ? topic.getCefrLevel().name() : level.name())
                        .build());
            }

            // 2. Speaking Module
            if (includeSpeaking) {
                List<Short> topicIds = List.of(topic.getId());
                long speakingCount = speakingScenarioRepository.findByCefrLevelAndTopicIdInAndIsActiveTrue(level, topicIds).size();
                if (speakingCount > 0) {
                    modules.add(RoadmapModule.builder()
                            .type("SPEAKING")
                            .title("Giao tiếp thực tế: " + topic.getNameVi())
                            .topicId(topic.getId())
                            .itemCount((int) speakingCount)
                            .cefrLevel(level.name())
                            .build());
                }
            }

            // Chỉ tạo milestone nếu có module
            if (!modules.isEmpty()) {
                milestones.add(RoadmapMilestone.builder()
                        .weekNumber(week)
                        .title("Tuần " + week + ": " + topic.getNameVi())
                        .description("Hoàn thiện từ vựng và kỹ năng liên quan đến chủ đề " + topic.getNameVi())
                        .modules(modules)
                        .build());
                week++;
            }
        }

        return milestones;
    }
}
