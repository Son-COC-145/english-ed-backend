package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import com.example.english_app.entity.enums.TopicCategory;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.service.adaptive.roadmap.RoadmapContentSnapshotService;
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
    private final RoadmapContentSnapshotService contentSnapshotService;

    /** Builds a roadmap without updating onboarding state; the durable worker owns persistence. */
    @Transactional(readOnly = true)
    public RoadmapResponse generateRoadmap(CefrLevel level, String goalSurveyJson) {
        GoalSurveyParser.ParsedGoalSurvey survey = goalSurveyParser.parse(goalSurveyJson);
        return buildRoadmap(level, survey.categories(), survey.learningGoal(), survey.focusSkills());
    }

    /**
     * Logic thuần tuý để lắp ráp lộ trình.
     */
    private RoadmapResponse buildRoadmap(
            CefrLevel level,
            List<TopicCategory> categories,
            LearningGoal learningGoal,
            List<LearnerSkill> focusSkills) {
        
        List<String> categoryNames = categories.stream().map(Enum::name).collect(Collectors.toList());
        
        // 1. Lấy Topics (ưu tiên đúng level và đúng category)
        List<Topic> topics = selectTopics(level, categoryNames);
        
        // 2. Lấy Speaking Scenarios (tuỳ vào focusSkills)
        boolean hasSpeaking = learningGoal == LearningGoal.COMMUNICATION
                || focusSkills.contains(LearnerSkill.SPEAKING);
        
        List<RoadmapMilestone> milestones = assembleMilestones(
                level, topics, hasSpeaking, learningGoal, focusSkills);

        return RoadmapResponse.builder()
                .cefrLevel(level.name())
                .totalWeeks(milestones.size())
                .milestones(milestones)
                .build();
    }

    private List<Topic> selectTopics(CefrLevel level, List<String> categories) {
        List<Topic> primaryTopics = topicRepository.findForRoadmap(level, categories, PageRequest.of(0, 5));
        
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

    private List<RoadmapMilestone> assembleMilestones(
            CefrLevel level,
            List<Topic> topics,
            boolean includeSpeaking,
            LearningGoal learningGoal,
            List<LearnerSkill> focusSkills) {
        List<RoadmapMilestone> milestones = new ArrayList<>();

        int week = 1;
        for (Topic topic : topics) {
            List<RoadmapModule> modules = new ArrayList<>();

            contentSnapshotService.vocabularyModule(topic, level).ifPresent(modules::add);
            if (includeSpeaking) {
                contentSnapshotService.speakingModule(topic, level).ifPresent(modules::add);
            }

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

        boolean includePronunciation = shouldIncludePronunciation(level, learningGoal, focusSkills);
        if (includePronunciation && milestones.isEmpty() && !topics.isEmpty()) {
            Topic topic = topics.getFirst();
            milestones.add(RoadmapMilestone.builder()
                    .weekNumber(1)
                    .title("Tuần 1: " + topic.getNameVi())
                    .description("Xây dựng nền tảng phát âm tiếng Anh")
                    .modules(new ArrayList<>())
                    .build());
        }
        if (includePronunciation && !milestones.isEmpty()) {
            distributeIpaModules(milestones, contentSnapshotService.ipaModuleGroups(level));
        }
        milestones.removeIf(milestone ->
                milestone.getModules() == null || milestone.getModules().isEmpty());

        return milestones;
    }

    private boolean shouldIncludePronunciation(
            CefrLevel level,
            LearningGoal learningGoal,
            List<LearnerSkill> focusSkills) {
        return level == CefrLevel.A1
                || level == CefrLevel.A2
                || learningGoal == LearningGoal.COMMUNICATION
                || focusSkills.contains(LearnerSkill.PRONUNCIATION)
                || focusSkills.contains(LearnerSkill.SPEAKING);
    }

    private void distributeIpaModules(
            List<RoadmapMilestone> milestones,
            List<RoadmapContentSnapshotService.IpaModuleGroup> groups) {
        int lastWeekIndex = milestones.size() - 1;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            int weekIndex = Math.min(groupIndex, lastWeekIndex);
            RoadmapMilestone milestone = milestones.get(weekIndex);
            if (milestone.getModules() == null) {
                milestone.setModules(new ArrayList<>());
            } else if (!(milestone.getModules() instanceof ArrayList<?>)) {
                milestone.setModules(new ArrayList<>(milestone.getModules()));
            }
            milestone.getModules().addAll(groups.get(groupIndex).modules());
        }
    }
}
