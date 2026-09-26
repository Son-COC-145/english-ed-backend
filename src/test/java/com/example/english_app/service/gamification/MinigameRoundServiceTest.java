package com.example.english_app.service.gamification;

import com.example.english_app.dto.response.MinigameRoundResponse;
import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.enums.MinigameRoundStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.MinigameRound;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.MinigameRoundRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinigameRoundServiceTest {
    @Mock private MinigameRoundRepository roundRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private MinigameRoundService service;

    private final User student = User.builder().id(7L).email("student@example.com").build();

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("student@example.com", null, List.of()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeComputesScoreFromCorrectAnswers() {
        MinigameRound round = round(MinigameRoundStatus.IN_PROGRESS, 3, 2);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(roundRepository.findByIdAndStudentIdForUpdate(11L, 7L)).thenReturn(Optional.of(round));
        when(roundRepository.save(round)).thenReturn(round);

        MinigameRoundResponse response = service.completeRound(11L);

        assertEquals(MinigameRoundStatus.COMPLETED, response.getStatus());
        assertEquals((short) 67, response.getScore());
        assertNotNull(response.getCompletedAt());
    }

    @Test
    void completeRejectsRoundWithoutAnswers() {
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(roundRepository.findByIdAndStudentIdForUpdate(11L, 7L))
                .thenReturn(Optional.of(round(MinigameRoundStatus.IN_PROGRESS, 0, 0)));

        AppException error = assertThrows(AppException.class, () -> service.completeRound(11L));

        assertEquals(ErrorCode.MINIGAME_ROUND_EMPTY, error.getErrorCode());
        verify(roundRepository, never()).save(any());
    }

    @Test
    void completingTwiceKeepsTheFirstScore() {
        MinigameRound round = round(MinigameRoundStatus.COMPLETED, 4, 4);
        round.setScore((short) 100);
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(student));
        when(roundRepository.findByIdAndStudentIdForUpdate(11L, 7L)).thenReturn(Optional.of(round));

        assertEquals((short) 100, service.completeRound(11L).getScore());
        verify(roundRepository, never()).save(any());
    }

    @Test
    void answerForWordOfAnotherTopicIsRejected() {
        when(roundRepository.findByIdAndStudentIdForUpdate(11L, 7L))
                .thenReturn(Optional.of(round(MinigameRoundStatus.IN_PROGRESS, 0, 0)));
        Vocabulary otherTopicWord = Vocabulary.builder().id(3L).topic(Topic.builder().id((short) 9).build()).build();

        AppException error = assertThrows(AppException.class,
                () -> service.lockOpenRound(7L, 11L, otherTopicWord, GameType.MATCHING_FLASH));

        assertEquals(ErrorCode.INVALID_REQUEST, error.getErrorCode());
    }

    @Test
    void answerForCompletedRoundIsRejected() {
        when(roundRepository.findByIdAndStudentIdForUpdate(11L, 7L))
                .thenReturn(Optional.of(round(MinigameRoundStatus.COMPLETED, 2, 1)));
        Vocabulary word = Vocabulary.builder().id(3L).topic(Topic.builder().id((short) 5).build()).build();

        AppException error = assertThrows(AppException.class,
                () -> service.lockOpenRound(7L, 11L, word, GameType.MATCHING_FLASH));

        assertEquals(ErrorCode.MINIGAME_ROUND_CLOSED, error.getErrorCode());
    }

    @Test
    void recordAnswerUpdatesCounters() {
        MinigameRound round = round(MinigameRoundStatus.IN_PROGRESS, 1, 1);

        service.recordAnswer(round, false, 0, 12);

        assertEquals(2, round.getTotalQuestions());
        assertEquals(1, round.getCorrectCount());
        assertEquals(12, round.getDurationSeconds());
        verify(roundRepository).save(round);
    }

    private MinigameRound round(MinigameRoundStatus status, int total, int correct) {
        return MinigameRound.builder().id(11L).student(student).topic(Topic.builder().id((short) 5).nameEn("Daily").build())
                .gameType(GameType.MATCHING_FLASH).status(status).totalQuestions(total).correctCount(correct).build();
    }
}
