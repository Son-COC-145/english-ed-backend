package com.example.english_app.service.onboarding;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    @Mock
    private OnboardingRepository onboardingRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private RoadmapGenerationService roadmapGenerationService;

    private StudentOnboarding mockOnboarding;
    private Topic mockTopic;

    @BeforeEach
    void setUp() {
        mockOnboarding = new StudentOnboarding();
        mockOnboarding.setId(100L);
        
        mockTopic = new Topic();
        mockTopic.setId((short) 1);
        mockTopic.setNameEn("Work");
        mockTopic.setNameVi("Công việc");
        mockTopic.setCefrLevel(CefrLevel.B1);
    }

    @Test
    void generateAndPersist_ShouldCreateRoadmapWithModules_WhenDataExists() throws Exception {
        // Arrange
        Long userId = 1L;
        String goalJson = "{}";
        CefrLevel level = CefrLevel.B1;
        
        when(goalSurveyParser.extractCategories(goalJson)).thenReturn(List.of(TopicCategory.WORK));
        when(goalSurveyParser.extractFocusSkills(goalJson)).thenReturn(List.of("Giao tiếp"));
        
        when(topicRepository.findForRoadmap(eq(level), any(), any())).thenReturn(List.of(mockTopic));
        
        when(vocabularyRepository.countByTopicId((short) 1)).thenReturn(20L);
        when(speakingScenarioRepository.findByCefrLevelAndTopicIdInAndIsActiveTrue(eq(level), any())).thenReturn(List.of()); // Returns empty speaking scenarios just for this test
        
        when(onboardingRepository.findByStudentId(userId)).thenReturn(Optional.of(mockOnboarding));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"dummy\":\"json\"}");
        
        // Act
        RoadmapResponse result = roadmapGenerationService.generateAndPersist(userId, level, goalJson);
        
        // Assert
        assertNotNull(result);
        assertEquals(CefrLevel.B1.name(), result.getCefrLevel());
        assertEquals(1, result.getTotalWeeks());
        assertEquals(1, result.getMilestones().size()); // Only 1 topic found
        assertEquals(1, result.getMilestones().get(0).getModules().size()); // Only vocab module added
        
        verify(onboardingRepository).save(mockOnboarding);
        assertEquals("{\"dummy\":\"json\"}", mockOnboarding.getRoadmapJson());
    }

    @Test
    void generateAndPersist_ShouldUseFallbackTopics_WhenPrimaryEmpty() throws Exception {
        // Arrange
        Long userId = 1L;
        String goalJson = "{}";
        CefrLevel level = CefrLevel.B1;
        
        when(goalSurveyParser.extractCategories(goalJson)).thenReturn(List.of(TopicCategory.WORK));
        when(goalSurveyParser.extractFocusSkills(goalJson)).thenReturn(List.of());
        
        // Return empty for first query, return mockTopic for fallback query
        when(topicRepository.findForRoadmap(eq(level), eq(List.of("WORK")), any())).thenReturn(List.of());
        when(topicRepository.findForRoadmap(eq(level), isNull(), any())).thenReturn(List.of(mockTopic));
        
        when(vocabularyRepository.countByTopicId((short) 1)).thenReturn(0L); // No vocab
        when(onboardingRepository.findByStudentId(userId)).thenReturn(Optional.of(mockOnboarding));
        
        // Act
        RoadmapResponse result = roadmapGenerationService.generateAndPersist(userId, level, goalJson);
        
        // Assert
        assertEquals(0, result.getTotalWeeks()); // Because no vocab & no speaking
        verify(topicRepository).findForRoadmap(eq(level), isNull(), any());
    }
}
