package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import com.example.english_app.entity.enums.TopicCategory;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.service.adaptive.roadmap.RoadmapContentSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapGenerationServiceTest {

    @Mock private GoalSurveyParser goalSurveyParser;
    @Mock private TopicRepository topicRepository;
    @Mock private RoadmapContentSnapshotService contentSnapshotService;
    @InjectMocks private RoadmapGenerationService service;

    private Topic topic;

    @BeforeEach
    void setUp() {
        topic = new Topic();
        topic.setId((short) 1);
        topic.setNameEn("Work");
        topic.setNameVi("Công việc");
        topic.setCefrLevel(CefrLevel.B1);
    }

    @Test
    void createsStableWeeklyRoadmapWithCefrContract() {
        when(goalSurveyParser.parse("{}"))
                .thenReturn(survey(LearningGoal.COMMUNICATION, List.of(LearnerSkill.SPEAKING)));
        when(topicRepository.findForRoadmap(eq(CefrLevel.B1), any(), any()))
                .thenReturn(List.of(topic));
        RoadmapModule vocabulary = module("VOCABULARY", "VOCABULARY:1", 1L, 2L);
        RoadmapModule speaking = module("SPEAKING", "SPEAKING:1", 3L);
        RoadmapModule ipa = module("IPA_PRONUNCIATION", "IPA:IPA_VOWELS_BASIC", 4L);
        when(contentSnapshotService.vocabularyModule(topic, CefrLevel.B1))
                .thenReturn(Optional.of(vocabulary));
        when(contentSnapshotService.speakingModule(topic, CefrLevel.B1))
                .thenReturn(Optional.of(speaking));
        when(contentSnapshotService.ipaModuleGroups(CefrLevel.B1))
                .thenReturn(List.of(new RoadmapContentSnapshotService.IpaModuleGroup(
                        "IPA_VOWELS_BASIC", List.of(ipa))));

        RoadmapResponse result = service.generateRoadmap(CefrLevel.B1, "{}");

        assertThat(result.getSchemaVersion()).isEqualTo(2);
        assertThat(result.getCurrentCefrLevel()).isEqualTo("B1");
        assertThat(result.getTargetCefrLevel()).isEqualTo("B2");
        assertThat(result.getTotalWeeks()).isEqualTo(1);
        assertThat(result.getMilestones().getFirst().getModules())
                .extracting(RoadmapModule::getModuleKey)
                .containsExactly("VOCABULARY:1", "SPEAKING:1", "IPA:IPA_VOWELS_BASIC");
    }

    @Test
    void dailyStudyBudgetDoesNotSplitWeeklyRoadmap() {
        when(goalSurveyParser.parse("{}"))
                .thenReturn(new GoalSurveyParser.ParsedGoalSurvey(
                        LearningGoal.WORK,
                        List.of(TopicCategory.WORK),
                        List.of(LearnerSkill.VOCABULARY),
                        5));
        when(topicRepository.findForRoadmap(eq(CefrLevel.B1), any(), any()))
                .thenReturn(List.of(topic));
        RoadmapModule vocabulary = module(
                "VOCABULARY",
                "VOCABULARY:1",
                java.util.stream.LongStream.rangeClosed(1, 12).boxed().toArray(Long[]::new));
        when(contentSnapshotService.vocabularyModule(topic, CefrLevel.B1))
                .thenReturn(Optional.of(vocabulary));

        RoadmapResponse result = service.generateRoadmap(CefrLevel.B1, "{}");

        assertThat(result.getTotalWeeks()).isEqualTo(1);
        assertThat(result.getMilestones().getFirst().getModules())
                .containsExactly(vocabulary);
        assertThat(vocabulary.getItemCount()).isEqualTo(12);
    }

    @Test
    void usesLevelFallbackWhenPreferredCategoriesHaveNoTopics() {
        when(goalSurveyParser.parse("{}"))
                .thenReturn(survey(LearningGoal.WORK, List.of()));
        when(topicRepository.findForRoadmap(eq(CefrLevel.B1), eq(List.of("WORK")), any()))
                .thenReturn(List.of());
        when(topicRepository.findForRoadmap(eq(CefrLevel.B1), isNull(), any()))
                .thenReturn(List.of(topic));
        when(contentSnapshotService.vocabularyModule(topic, CefrLevel.B1))
                .thenReturn(Optional.empty());

        RoadmapResponse result = service.generateRoadmap(CefrLevel.B1, "{}");

        assertThat(result.getMilestones()).isEmpty();
        verify(topicRepository).findForRoadmap(eq(CefrLevel.B1), isNull(), any());
    }

    @Test
    void distributesRemainingIpaGroupsIntoLastAvailableWeek() {
        Topic secondTopic = new Topic();
        secondTopic.setId((short) 2);
        secondTopic.setNameVi("Du lịch");
        secondTopic.setCefrLevel(CefrLevel.A1);
        when(goalSurveyParser.parse("{}"))
                .thenReturn(survey(LearningGoal.WORK, List.of(LearnerSkill.PRONUNCIATION)));
        when(topicRepository.findForRoadmap(eq(CefrLevel.A1), any(), any()))
                .thenReturn(List.of(topic, secondTopic));
        when(contentSnapshotService.vocabularyModule(any(), eq(CefrLevel.A1)))
                .thenAnswer(invocation -> {
                    Topic selected = invocation.getArgument(0);
                    return Optional.of(module(
                            "VOCABULARY",
                            "VOCABULARY:" + selected.getId(),
                            selected.getId().longValue()));
                });
        when(contentSnapshotService.ipaModuleGroups(CefrLevel.A1)).thenReturn(List.of(
                ipaGroup("G1"), ipaGroup("G2"), ipaGroup("G3"), ipaGroup("G4")));

        RoadmapResponse result = service.generateRoadmap(CefrLevel.A1, "{}");

        assertThat(result.getTotalWeeks()).isEqualTo(2);
        assertThat(result.getMilestones().getFirst().getModules())
                .extracting(RoadmapModule::getModuleKey)
                .containsExactly("VOCABULARY:1", "IPA:G1");
        assertThat(result.getMilestones().get(1).getModules())
                .extracting(RoadmapModule::getModuleKey)
                .containsExactly("VOCABULARY:2", "IPA:G2", "IPA:G3", "IPA:G4");
    }

    private RoadmapContentSnapshotService.IpaModuleGroup ipaGroup(String key) {
        return new RoadmapContentSnapshotService.IpaModuleGroup(
                key, List.of(module("IPA_PRONUNCIATION", "IPA:" + key, 1L)));
    }

    private RoadmapModule module(String type, String key, Long... ids) {
        List<Long> contentIds = List.of(ids);
        return RoadmapModule.builder()
                .type(type)
                .title(key)
                .moduleKey(key)
                .contentItemIds(contentIds)
                .contentVersion("version-" + key)
                .itemCount(contentIds.size())
                .build();
    }

    private GoalSurveyParser.ParsedGoalSurvey survey(
            LearningGoal goal,
            List<LearnerSkill> focusSkills) {
        return new GoalSurveyParser.ParsedGoalSurvey(
                goal, List.of(TopicCategory.WORK), focusSkills, 15);
    }
}
