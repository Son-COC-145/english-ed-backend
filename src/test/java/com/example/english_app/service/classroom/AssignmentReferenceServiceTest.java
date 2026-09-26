package com.example.english_app.service.classroom;

import com.example.english_app.dto.response.classroom.SubmissionResultResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.enums.MinigameRoundStatus;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.vocabulary.MinigameRound;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.MinigameRoundRepository;
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
    @Mock private MinigameRoundRepository roundRepository;
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
    void acceptsCompletedRoundOfTheAssignedTopic() {
        Assignment assignment = vocabularyAssignment((short) 5);
        when(roundRepository.findById(11L)).thenReturn(Optional.of(round((short) 5, MinigameRoundStatus.COMPLETED)));

        assertDoesNotThrow(() -> service.validateResult(assignment, 7L, 11L));
    }

    @Test
    void rejectsRoundThatIsStillInProgress() {
        Assignment assignment = vocabularyAssignment((short) 5);
        when(roundRepository.findById(11L)).thenReturn(Optional.of(round((short) 5, MinigameRoundStatus.IN_PROGRESS)));

        AppException error = assertThrows(AppException.class, () -> service.validateResult(assignment, 7L, 11L));

        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
    }

    @Test
    void rejectsRoundOfAnotherTopic() {
        Assignment assignment = vocabularyAssignment((short) 5);
        when(roundRepository.findById(11L)).thenReturn(Optional.of(round((short) 6, MinigameRoundStatus.COMPLETED)));

        assertThrows(AppException.class, () -> service.validateResult(assignment, 7L, 11L));
    }

    @Test
    void describesVocabularyRoundWithCorrectAnswers() {
        when(roundRepository.findById(11L)).thenReturn(Optional.of(round((short) 5, MinigameRoundStatus.COMPLETED)));

        SubmissionResultResponse result = service.describeResult(ModuleType.VOCABULARY, 11L).orElseThrow();

        assertEquals(80, result.getOverallScore());
        assertEquals(8, result.getCorrectCount());
        assertEquals(10, result.getTotalQuestions());
        assertEquals("MATCHING_FLASH", result.getGameType());
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

    private Assignment vocabularyAssignment(short topicId) {
        return Assignment.builder().id(4L).moduleType(ModuleType.VOCABULARY).refId((long) topicId).build();
    }

    private MinigameRound round(short topicId, MinigameRoundStatus status) {
        return MinigameRound.builder().id(11L).student(User.builder().id(7L).build())
                .topic(Topic.builder().id(topicId).build()).gameType(GameType.MATCHING_FLASH).status(status)
                .totalQuestions(10).correctCount(8).score((short) 80).xpEarned(40).durationSeconds(120).build();
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
