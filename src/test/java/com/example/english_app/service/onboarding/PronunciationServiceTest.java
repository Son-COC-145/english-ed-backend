package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.entity.question.Question;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.service.audio.AudioAssessmentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PronunciationServiceTest {

    @Mock
    private AudioAssessmentPort audioAssessmentPort;

    @Mock
    private PlacementTestSessionRepository sessionRepository;

    @Mock
    private PlacementTestAnswerRepository answerRepository;

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private PronunciationService pronunciationService;

    private PlacementTestSession mockSession;
    private User mockUser;
    private Question mockQuestion;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).build();
        mockSession = PlacementTestSession.builder()
                .id(100L)
                .student(mockUser)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .isCompleted(false)
                .build();
        mockQuestion = Question.builder().id(200L).build();
    }

    @Test
    void submitPronunciation_ShouldSaveCorrectAnswer_WhenScoreIs60OrHigher() throws Exception {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(questionRepository.findById(200L)).thenReturn(Optional.of(mockQuestion));
        
        PronunciationScoreResult scoreResult = PronunciationScoreResult.builder()
                .word("hello")
                .overallScore((short) 65)
                .status("SCORED")
                .build();
                
        when(audioAssessmentPort.assess(any(byte[].class), eq("hello"))).thenReturn(scoreResult);
        
        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", "dummy_audio".getBytes());

        // Act
        PronunciationScoreResult result = pronunciationService.submitPronunciation(1L, 100L, 200L, audioFile, "hello", 1);

        // Assert
        assertEquals((short) 65, result.getOverallScore());
        
        ArgumentCaptor<PlacementTestAnswer> answerCaptor = ArgumentCaptor.forClass(PlacementTestAnswer.class);
        verify(answerRepository).save(answerCaptor.capture());
        
        PlacementTestAnswer savedAnswer = answerCaptor.getValue();
        assertTrue(savedAnswer.getIsCorrect());
        // Service hiện tại set question từ questionRepository — assertNotNull thay vì assertNull cũ
        assertNotNull(savedAnswer.getQuestion());
        assertEquals(200L, savedAnswer.getQuestion().getId());
    }

    @Test
    void submitPronunciation_ShouldSaveIncorrectAnswer_WhenScoreIsBelow60() throws Exception {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        when(questionRepository.findById(200L)).thenReturn(Optional.of(mockQuestion));
        
        PronunciationScoreResult scoreResult = PronunciationScoreResult.builder()
                .word("hello")
                .overallScore((short) 59)
                .status("SCORED")
                .build();
                
        when(audioAssessmentPort.assess(any(byte[].class), eq("hello"))).thenReturn(scoreResult);
        
        MockMultipartFile audioFile = new MockMultipartFile("audioFile", "test.wav", "audio/wav", "dummy_audio".getBytes());

        // Act
        pronunciationService.submitPronunciation(1L, 100L, 200L, audioFile, "hello", 1);

        // Assert
        ArgumentCaptor<PlacementTestAnswer> answerCaptor = ArgumentCaptor.forClass(PlacementTestAnswer.class);
        verify(answerRepository).save(answerCaptor.capture());
        
        PlacementTestAnswer savedAnswer = answerCaptor.getValue();
        assertFalse(savedAnswer.getIsCorrect());
    }

    @Test
    void submitPronunciation_ShouldThrowException_WhenSessionBelongsToAnotherUser() {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(mockSession));
        MockMultipartFile audioFile = new MockMultipartFile("audio", "audio".getBytes());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            pronunciationService.submitPronunciation(2L, 100L, 200L, audioFile, "hello", 1); // User 2 trying to access user 1's session
        });
        assertEquals(1005, exception.getErrorCode().getCode()); // ACCESS_DENIED
    }
}
