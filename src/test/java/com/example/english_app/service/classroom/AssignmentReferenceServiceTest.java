package com.example.english_app.service.classroom;

import com.example.english_app.dto.response.classroom.SubmissionResultResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.MinigameResultRepository;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentReferenceServiceTest {
    @Mock private IpaExampleWordRepository wordRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private SpeakingScenarioRepository scenarioRepository;
    @Mock private PronunciationPracticeLogRepository practiceRepository;
    @Mock private MinigameResultRepository gameRepository;
    @Mock private SpeakingSessionRepository sessionRepository;
    @InjectMocks private AssignmentReferenceService service;

    @Test
    void acceptsIpaPracticeLogForPronunciationAssignment() {
        Assignment assignment = pronunciationAssignment(42L);
        when(practiceRepository.findById(8L)).thenReturn(Optional.of(practiceLog(42L)));

        assertDoesNotThrow(() -> service.validateResult(assignment, 7L, 8L));
    }

    @Test
    void rejectsPracticeLogOfAnotherWord() {
        Assignment assignment = pronunciationAssignment(42L);
        when(practiceRepository.findById(8L)).thenReturn(Optional.of(practiceLog(43L)));

        AppException error = assertThrows(AppException.class, () -> service.validateResult(assignment, 7L, 8L));

        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
    }

    @Test
    void describesPronunciationResultScores() {
        when(practiceRepository.findById(8L)).thenReturn(Optional.of(practiceLog(42L)));

        SubmissionResultResponse result = service.describeResult(ModuleType.PRONUNCIATION, 8L).orElseThrow();

        assertEquals(8L, result.getResultId());
        assertEquals(88, result.getOverallScore());
        assertEquals(70, result.getFluencyScore());
        assertNull(result.getTaskCompletionScore());
    }

    @Test
    void describeResultIsEmptyWhenResultWasRemoved() {
        when(sessionRepository.findById(9L)).thenReturn(Optional.empty());

        assertTrue(service.describeResult(ModuleType.SPEAKING, 9L).isEmpty());
    }

    private Assignment pronunciationAssignment(Long wordId) {
        return Assignment.builder().id(3L).moduleType(ModuleType.PRONUNCIATION).refId(wordId).build();
    }

    private PronunciationPracticeLog practiceLog(Long wordId) {
        return PronunciationPracticeLog.builder().id(8L).student(User.builder().id(7L).build())
                .practiceType(PracticeType.IPA_PHONEME).refId(wordId)
                .overallScore((short) 88).fluencyScore((short) 70).completenessScore((short) 90).build();
    }
}
