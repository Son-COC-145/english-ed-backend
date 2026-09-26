package com.example.english_app.service.gamification;

import com.example.english_app.dto.request.MinigameRoundStartRequest;
import com.example.english_app.dto.response.MinigameRoundResponse;
import com.example.english_app.entity.enums.MinigameRoundStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.MinigameRound;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.entity.enums.GameType;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.MinigameRoundRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Mini-game rounds: start a round for a topic, attach each answer to it (via the minigame submit API),
 * then complete it to get a 0–100 score. A COMPLETED round is what a VOCABULARY assignment is submitted with.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MinigameRoundService {
    private final MinigameRoundRepository roundRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;

    @Transactional
    public MinigameRoundResponse startRound(MinigameRoundStartRequest request) {
        User student = getCurrentUser();
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());
        MinigameRound round = roundRepository.save(MinigameRound.builder()
                .student(student)
                .topic(topic)
                .gameType(request.getGameType())
                .build());
        return toResponse(round);
    }

    public MinigameRoundResponse getRound(Long roundId) {
        User student = getCurrentUser();
        return toResponse(roundRepository.findByIdAndStudentId(roundId, student.getId())
                .orElseThrow(() -> ErrorCode.MINIGAME_ROUND_NOT_FOUND.toException()));
    }

    /** Closes the round and computes its score; completing an already completed round returns it unchanged. */
    @Transactional
    public MinigameRoundResponse completeRound(Long roundId) {
        User student = getCurrentUser();
        MinigameRound round = roundRepository.findByIdAndStudentIdForUpdate(roundId, student.getId())
                .orElseThrow(() -> ErrorCode.MINIGAME_ROUND_NOT_FOUND.toException());
        if (round.getStatus() == MinigameRoundStatus.COMPLETED) {
            return toResponse(round);
        }
        if (round.getTotalQuestions() == 0) {
            throw ErrorCode.MINIGAME_ROUND_EMPTY.toException();
        }
        round.setScore((short) Math.round(round.getCorrectCount() * 100.0 / round.getTotalQuestions()));
        round.setStatus(MinigameRoundStatus.COMPLETED);
        round.setCompletedAt(LocalDateTime.now());
        return toResponse(roundRepository.save(round));
    }

    /**
     * Locks an open round of the student for a new answer. The answered word must belong to the round's topic
     * and the game type must match, otherwise the answer is rejected before any XP/progress is written.
     */
    @Transactional
    public MinigameRound lockOpenRound(Long studentId, Long roundId, Vocabulary vocabulary, GameType gameType) {
        MinigameRound round = roundRepository.findByIdAndStudentIdForUpdate(roundId, studentId)
                .orElseThrow(() -> ErrorCode.MINIGAME_ROUND_NOT_FOUND.toException());
        if (round.getStatus() != MinigameRoundStatus.IN_PROGRESS) {
            throw ErrorCode.MINIGAME_ROUND_CLOSED.toException();
        }
        boolean sameTopic = vocabulary.getTopic() != null && vocabulary.getTopic().getId().equals(round.getTopic().getId());
        if (!sameTopic || round.getGameType() != gameType) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        return round;
    }

    @Transactional
    public void recordAnswer(MinigameRound round, boolean correct, int xpEarned, int durationSeconds) {
        round.setTotalQuestions(round.getTotalQuestions() + 1);
        if (correct) {
            round.setCorrectCount(round.getCorrectCount() + 1);
        }
        round.setXpEarned(round.getXpEarned() + xpEarned);
        round.setDurationSeconds(round.getDurationSeconds() + Math.max(0, durationSeconds));
        roundRepository.save(round);
    }

    private MinigameRoundResponse toResponse(MinigameRound round) {
        return MinigameRoundResponse.builder()
                .id(round.getId())
                .topicId(round.getTopic().getId())
                .topicName(round.getTopic().getNameEn())
                .gameType(round.getGameType())
                .status(round.getStatus())
                .totalQuestions(round.getTotalQuestions())
                .correctCount(round.getCorrectCount())
                .score(round.getScore())
                .xpEarned(round.getXpEarned())
                .durationSeconds(round.getDurationSeconds())
                .startedAt(round.getStartedAt())
                .completedAt(round.getCompletedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }
}
