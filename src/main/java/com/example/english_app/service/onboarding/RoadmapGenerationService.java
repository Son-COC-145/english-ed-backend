package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.TopicCategory;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service độc lập chịu trách nhiệm sinh lộ trình học tập (Roadmap) dựa trên
 * mục tiêu và trình độ của học viên.
 *
 * Worker bền vững chịu trách nhiệm retry và publish kết quả; service này chỉ dựng dữ liệu.
 */
@Service
@RequiredArgsConstructor
public class RoadmapGenerationService {

    private final GoalSurveyParser goalSurveyParser;
    private final TopicRepository topicRepository;
    private final VocabularyRepository vocabularyRepository;
    private final SpeakingScenarioRepository speakingScenarioRepository;

    /** Builds a roadmap without updating onboarding state; the durable worker owns persistence. */
    @Transactional(readOnly = true)
    public RoadmapResponse generateRoadmap(CefrLevel level, String goalSurveyJson) {
        List<TopicCategory> categories = goalSurveyParser.extractCategories(goalSurveyJson);
        List<String> focusSkills = goalSurveyParser.extractFocusSkills(goalSurveyJson);
        return buildRoadmap(level, categories, focusSkills);
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
        
        List<RoadmapMilestone> milestones = assembleMilestones(level, topics, hasSpeaking, focusSkills);

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

    private List<RoadmapMilestone> assembleMilestones(CefrLevel level, List<Topic> topics, boolean includeSpeaking, List<String> focusSkills) {
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

            // 3. IPA Module (Module 1) - Chỉ thêm vào Tuần 1
            if (week == 1 && (level == CefrLevel.A1 || level == CefrLevel.A2 || (focusSkills != null && focusSkills.stream().anyMatch(s -> s.equalsIgnoreCase("Phát âm") || s.equalsIgnoreCase("Giao tiếp"))))) {
                modules.add(RoadmapModule.builder()
                        .type("IPA_PRONUNCIATION")
                        .title("Nền tảng phát âm IPA")
                        .topicId(null)
                        .itemCount(44)
                        .cefrLevel(level.name())
                        .build());
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
