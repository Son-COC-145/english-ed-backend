package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.TopicCategory;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
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
    private VocabularyRepository vocabularyRepository;
    @Mock
    private SpeakingScenarioRepository speakingScenarioRepository;
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
        
        when(vocabularyRepository.countByTopicId((short) 1)).thenReturn(20L);
        when(speakingScenarioRepository.findByCefrLevelAndTopicIdInAndIsActiveTrue(eq(level), any())).thenReturn(List.of()); // Returns empty speaking scenarios just for this test
        
        // Act
        RoadmapResponse result = roadmapGenerationService.generateRoadmap(level, goalJson);
        
        // Assert
        assertNotNull(result);
        assertEquals(CefrLevel.B1.name(), result.getCefrLevel());
        assertEquals(1, result.getTotalWeeks());
        assertEquals(1, result.getMilestones().size()); // Only 1 topic found
        assertEquals(2, result.getMilestones().get(0).getModules().size()); // Vocab + IPA module added for week 1
        assertEquals("VOCABULARY", result.getMilestones().get(0).getModules().get(0).getType());
        assertEquals("IPA_PRONUNCIATION", result.getMilestones().get(0).getModules().get(1).getType());
        
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
        
        when(vocabularyRepository.countByTopicId((short) 1)).thenReturn(0L); // No vocab
        // Act
        RoadmapResponse result = roadmapGenerationService.generateRoadmap(level, goalJson);
        
        // Assert
        assertEquals(0, result.getTotalWeeks()); // Because no vocab & no speaking
        verify(topicRepository).findForRoadmap(eq(level), isNull(), any());
    }
}
