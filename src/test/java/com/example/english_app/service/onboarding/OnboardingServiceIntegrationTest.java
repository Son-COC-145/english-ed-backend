package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementTestServiceIntegrationTest {

    @Mock private OnboardingRepository           onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private PlacementTestAnswerRepository  answerRepository;
    @Mock private QuestionRepository             questionRepository;
    @Mock private UserRepository                 userRepository;
    @Mock private RoadmapGenerationService       roadmapGenerationService;
    @Mock private PlacementResultFactory         resultFactory;
    @Mock private ObjectMapper                   objectMapper;

    @InjectMocks
    private PlacementTestService placementTestService;

    private PlacementTestSession mockSession;
    private StudentOnboarding    mockOnboarding;
    private User                 mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).build();

        mockSession = PlacementTestSession.builder()
                .id(100L)
                .student(mockUser)
                .isCompleted(false)
                .currentCefrEstimate(CefrLevel.B1)
                .build();

        mockOnboarding = StudentOnboarding.builder()
                .student(mockUser)
                .goalSurveyJson("{}")
                .build();
    }

    @Test
    void completeTest_ShouldNotRollback_WhenRoadmapFails() {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).thenReturn(Collections.emptyList());
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(mockOnboarding));
        when(resultFactory.calculateAllSkills(any())).thenReturn(
                PlacementResultFactory.SkillScores.builder()
                        .vocab((short) 0).grammar((short) 0).reading((short) 0)
                        .listening((short) 0).pronunciation((short) 0).build());
        when(resultFactory.buildResponse(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(PlacementResultResponse.builder()
                        .cefrLevel("B1").roadmapGenerated(false).build());

        // Simulate RoadmapGenerationService throwing an exception
        when(roadmapGenerationService.generateAndPersist(eq(1L), eq(CefrLevel.B1), eq("{}")))
                .thenThrow(new RuntimeException("Simulated roadmap failure"));

        // Act
        PlacementResultResponse result = placementTestService.completeTest(100L, 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isRoadmapGenerated());
        assertEquals(CefrLevel.B1.name(), result.getCefrLevel());

        // Ensure onboarding was still saved despite roadmap failure
        verify(onboardingRepository, times(1)).save(any(StudentOnboarding.class));
    }
}
