package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.enums.CefrLevel;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoadmapGenerationServiceTest {

    @Mock
    private GoalSurveyParser goalSurveyParser;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private RoadmapContentSnapshotService contentSnapshotService;
    @InjectMocks
    private RoadmapGenerationService roadmapGenerationService;

    private Topic mockTopic;

    @BeforeEach
    void setUp() {
        mockTopic = new Topic();
        mockTopic.setId((short) 1);
        mockTopic.setNameEn("Work");
        mockTopic.setNameVi("Công việc");
        mockTopic.setCefrLevel(CefrLevel.B1);
    }

    @Test
    void generateRoadmap_ShouldCreateRoadmapWithModules_WhenDataExists() {
        // Arrange
        String goalJson = "{}";
        CefrLevel level = CefrLevel.B1;
        
        when(goalSurveyParser.extractCategories(goalJson)).thenReturn(List.of(TopicCategory.WORK));
        when(goalSurveyParser.extractFocusSkills(goalJson)).thenReturn(List.of("Giao tiếp"));
        
        when(topicRepository.findForRoadmap(eq(level), any(), any())).thenReturn(List.of(mockTopic));
        
        RoadmapModule vocabulary = RoadmapModule.builder()
                .type("VOCABULARY")
                .title("Từ vựng: Công việc")
                .moduleKey("VOCABULARY:1")
                .contentItemIds(List.of(1L, 2L))
                .contentVersion("v1")
                .itemCount(2)
                .build();
        RoadmapModule ipa = RoadmapModule.builder()
                .type("IPA_PRONUNCIATION")
                .title("Nguyên âm đơn IPA")
                .moduleKey("IPA:IPA_VOWELS_BASIC")
                .contentItemIds(List.of(1L))
                .contentVersion("i1")
                .itemCount(1)
                .build();
        when(contentSnapshotService.vocabularyModule(mockTopic, level)).thenReturn(java.util.Optional.of(vocabulary));
        when(contentSnapshotService.speakingModule(mockTopic, level)).thenReturn(java.util.Optional.empty());
        when(contentSnapshotService.ipaModuleGroups(level)).thenReturn(List.of(
                new RoadmapContentSnapshotService.IpaModuleGroup("IPA_VOWELS_BASIC", List.of(ipa))));
        
        // Act
        RoadmapResponse result = roadmapGenerationService.generateRoadmap(level, goalJson);
        
        // Assert
        assertNotNull(result);
        assertEquals(CefrLevel.B1.name(), result.getCefrLevel());
        assertEquals(1, result.getTotalWeeks());
        assertEquals(1, result.getMilestones().size()); // Only 1 topic found
        assertEquals(2, result.getMilestones().get(0).getModules().size());
        assertEquals("VOCABULARY", result.getMilestones().get(0).getModules().get(0).getType());
        assertEquals("IPA_PRONUNCIATION", result.getMilestones().get(0).getModules().get(1).getType());
        assertEquals("VOCABULARY:1", result.getMilestones().get(0).getModules().get(0).getModuleKey());
        
    }

    @Test
    void generateRoadmap_ShouldUseFallbackTopics_WhenPrimaryEmpty() {
        // Arrange
        String goalJson = "{}";
        CefrLevel level = CefrLevel.B1;
        
        when(goalSurveyParser.extractCategories(goalJson)).thenReturn(List.of(TopicCategory.WORK));
        when(goalSurveyParser.extractFocusSkills(goalJson)).thenReturn(List.of());
        
        // Return empty for first query, return mockTopic for fallback query
        when(topicRepository.findForRoadmap(eq(level), eq(List.of("WORK")), any())).thenReturn(List.of());
        when(topicRepository.findForRoadmap(eq(level), isNull(), any())).thenReturn(List.of(mockTopic));
        
        when(contentSnapshotService.vocabularyModule(mockTopic, level)).thenReturn(java.util.Optional.empty());
        // Act
        RoadmapResponse result = roadmapGenerationService.generateRoadmap(level, goalJson);
        
        // Assert
        assertEquals(0, result.getTotalWeeks()); // Because no vocab & no speaking
        verify(topicRepository).findForRoadmap(eq(level), isNull(), any());
    }

    @Test
    void generateRoadmap_ShouldMoveRemainingIpaGroupsToLastWeek_WhenRoadmapIsShort() {
        Topic secondTopic = new Topic();
        secondTopic.setId((short) 2);
        secondTopic.setNameVi("Du lịch");
        secondTopic.setCefrLevel(CefrLevel.A1);
        when(goalSurveyParser.extractCategories("{}"))
                .thenReturn(List.of(TopicCategory.WORK));
        when(goalSurveyParser.extractFocusSkills("{}"))
                .thenReturn(List.of("Phát âm"));
        when(topicRepository.findForRoadmap(eq(CefrLevel.A1), any(), any()))
                .thenReturn(List.of(mockTopic, secondTopic));
        when(contentSnapshotService.vocabularyModule(any(), eq(CefrLevel.A1)))
                .thenAnswer(invocation -> {
                    Topic topic = invocation.getArgument(0);
                    return java.util.Optional.of(RoadmapModule.builder()
                            .type("VOCABULARY")
                            .title("Từ vựng")
                            .moduleKey("VOCABULARY:" + topic.getId())
                            .contentItemIds(List.of(topic.getId().longValue()))
                            .contentVersion("v" + topic.getId())
                            .itemCount(1)
                            .build());
                });
        when(contentSnapshotService.ipaModuleGroups(CefrLevel.A1)).thenReturn(List.of(
                ipaGroup("G1"), ipaGroup("G2"), ipaGroup("G3"), ipaGroup("G4")));

        RoadmapResponse result = roadmapGenerationService.generateRoadmap(CefrLevel.A1, "{}");

        assertEquals(2, result.getTotalWeeks());
        assertEquals(List.of("IPA:G1"), result.getMilestones().get(0).getModules().stream()
                .filter(module -> "IPA_PRONUNCIATION".equals(module.getType()))
                .map(RoadmapModule::getModuleKey)
                .toList());
        assertEquals(List.of("IPA:G2", "IPA:G3", "IPA:G4"),
                result.getMilestones().get(1).getModules().stream()
                        .filter(module -> "IPA_PRONUNCIATION".equals(module.getType()))
                        .map(RoadmapModule::getModuleKey)
                        .toList());
    }

    private RoadmapContentSnapshotService.IpaModuleGroup ipaGroup(String key) {
        RoadmapModule module = RoadmapModule.builder()
                .type("IPA_PRONUNCIATION")
                .title(key)
                .moduleKey("IPA:" + key)
                .contentItemIds(List.of(1L))
                .contentVersion(key)
                .itemCount(1)
                .build();
        return new RoadmapContentSnapshotService.IpaModuleGroup(key, List.of(module));
    }
}
