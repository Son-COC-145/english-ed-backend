package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceIntegrationTest {

    @Mock
    private OnboardingRepository onboardingRepository;
    @Mock
    private PlacementTestSessionRepository sessionRepository;
    @Mock
    private PlacementTestAnswerRepository answerRepository;
    @Mock
    private RoadmapGenerationService roadmapGenerationService;

    @InjectMocks
    private OnboardingService onboardingService;

    private PlacementTestSession mockSession;
    private StudentOnboarding mockOnboarding;
    private User mockUser;

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
    void completePlacementTest_ShouldNotRollback_WhenRoadmapFails() {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).thenReturn(Collections.emptyList());
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(mockOnboarding));
        
        // Simulate RoadmapGenerationService throwing an exception
        when(roadmapGenerationService.generateAndPersist(eq(1L), eq(CefrLevel.B1), eq("{}")))
                .thenThrow(new RuntimeException("Simulated roadmap failure"));

        // Act
        PlacementResultResponse result = onboardingService.completePlacementTest(100L, 1L);

        // Assert
        assertNotNull(result);
        assertFalse(result.isRoadmapGenerated());
        assertEquals(CefrLevel.B1.name(), result.getCefrLevel());
        
        // Ensure onboarding was still saved despite roadmap failure
        verify(onboardingRepository, times(1)).save(any(StudentOnboarding.class));
    }
}
