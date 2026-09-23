package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.entity.question.Question;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.service.audio.AudioAssessmentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Service xử lý toàn bộ nghiệp vụ đánh giá phát âm trong Placement Test.
 *
 * <p><b>Trách nhiệm:</b> Class này chỉ biết về Pronunciation trong ngữ cảnh Placement Test.
 * Nó KHÔNG biết về CAT algorithm, goal survey, hay settings — những thứ đó
 * thuộc về {@link OnboardingLifecycleService}.
 *
 * <p><b>Dependency flow (một chiều):</b>
 * {@code OnboardingController} → {@code PronunciationService} → {@code AudioAssessmentPort}
 * {@code OnboardingController} → {@code OnboardingLifecycleService}
 * {@code OnboardingLifecycleService} KHÔNG phụ thuộc vào {@code PronunciationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationService {

    private static final int PASSING_SCORE = 60;

    private final AudioAssessmentPort audioAssessmentPort;
    private final PlacementTestSessionRepository sessionRepository;
    private final PlacementTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final PlacementTestService placementTestService;

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Entry point đầy đủ (P0 Section 3.5): Nhận audio, đánh giá phát âm, lưu DB,
     * cập nhật CAT state và trả về CẢ điểm số VÀ câu tiếp theo / kết quả hoàn tất.
     */
    @Transactional
    public com.example.english_app.dto.response.PlacementPronunciationAnswerResponse submitPronunciationWithProgression(
            Long userId,
            Long sessionId,
            Long questionId,
            MultipartFile audioFile,
            String word,
            int wordIndex) {

        // 1. Validate session
        PlacementTestSession session = validateSessionForPronunciation(sessionId, userId);

        // Guard: duplicate submission (Flutter retry khi timeout mạng sẽ gửi lại request)
        // Không có guard này → chấm điểm 2 lần + tăng currentQuestionIndex 2 lần → điểm lệch.
        if (answerRepository.existsBySessionIdAndQuestionId(sessionId, questionId)) {
            log.warn("[PronunciationService] Duplicate answer detected: sessionId={}, questionId={}, userId={}",
                    sessionId, questionId, userId);
            throw ErrorCode.ANSWER_ALREADY_SUBMITTED.toException();
        }

        // Fetch question
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // 2. Đọc audio bytes
        byte[] audioBytes = readAudioBytes(audioFile);

        // 3. Gọi Azure
        PronunciationScoreResult result = audioAssessmentPort.assess(audioBytes, word);

        log.info("Pronunciation assessed: userId={}, sessionId={}, word='{}', wordIndex={}, status={}, score={}",
                userId, sessionId, word, wordIndex, result.getStatus(), result.getOverallScore());

        // 4. Ghi kết quả vào PlacementTestAnswer
        boolean isCorrect = result.getOverallScore() != null && result.getOverallScore() >= PASSING_SCORE;
        recordPronunciationAnswer(session, question, word, result);

        // 5. Cập nhật lastActivityAt & counter & CAT state
        session.setLastActivityAt(LocalDateTime.now());
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        placementTestService.updateSkillCatState(session, question.getSkill(), isCorrect);
        sessionRepository.save(session);

        int answeredCount = session.getCurrentQuestionIndex();
        boolean shouldFinish = answeredCount >= placementTestService.getMaxPlacementQuestions();

        if (shouldFinish) {
            com.example.english_app.dto.response.PlacementResultResponse placementResult =
                    placementTestService.completeTest(session.getId(), userId);
            return com.example.english_app.dto.response.PlacementPronunciationAnswerResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(questionId)
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(placementResult)
                    .pronunciationResult(result)
                    .nextQuestion(null)
                    .build();
        }

        com.example.english_app.dto.response.PlacementQuestionResponse nextQuestion =
                placementTestService.getNextQuestion(session.getId(), userId);
        nextQuestion.setSubmittedQuestionId(questionId);
        nextQuestion.setPreviousAnswerCorrect(isCorrect);

        return com.example.english_app.dto.response.PlacementPronunciationAnswerResponse.builder()
                .sessionId(session.getId())
                .submittedQuestionId(questionId)
                .sessionStatus("IN_PROGRESS")
                .isTestCompleted(false)
                .pronunciationResult(result)
                .nextQuestion(nextQuestion)
                .build();
    }

    /**
     * Legacy entry point trả về chỉ PronunciationScoreResult.
     */
    @Transactional
    public PronunciationScoreResult submitPronunciation(
            Long userId,
            Long sessionId,
            Long questionId,
            MultipartFile audioFile,
            String word,
            int wordIndex) {

        return submitPronunciationWithProgression(userId, sessionId, questionId, audioFile, word, wordIndex)
                .getPronunciationResult();
    }

    // ─── Session Validation ───────────────────────────────────────────────────

    /**
     * Validates rằng session tồn tại, thuộc về user, chưa hoàn thành, và chưa timeout.
     *
     * <p>Cố tình sử dụng cùng logic với {@link OnboardingService#getNextQuestion}
     * để đảm bảo guard nhất quán — không tạo ra dependency vào OnboardingService.
     *
     * @throws AppException SESSION_NOT_FOUND nếu không tìm thấy.
     * @throws AppException ACCESS_DENIED     nếu session không thuộc về user.
     * @throws AppException PLACEMENT_TEST_ALREADY_COMPLETED nếu đã hoàn thành.
     * @throws AppException PLACEMENT_TEST_EXPIRED nếu session đã timeout.
     */
    private PlacementTestSession validateSessionForPronunciation(Long sessionId, Long userId) {
        PlacementTestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        // Guard: ownership
        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        // Guard: đã completed
        if (Boolean.TRUE.equals(session.getIsCompleted())) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        // Guard: timeout — dùng method trên entity (single source of truth)
        if (session.isExpired()) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }

        return session;
    }


    // ─── Answer Recording ─────────────────────────────────────────────────────

    /**
     * Lưu kết quả đánh giá phát âm vào bảng placement_test_answers.
     *
     * <p>Mapping:
     * <ul>
     *   <li>skill = PRONUNCIATION
     *   <li>answer_given = từ đã phát âm (reference text)
     *   <li>is_correct = overallScore ≥ 60 (hoặc null nếu UNAVAILABLE → false)
     *   <li>question = câu hỏi đã fetch từ DB (nullable FK cho phép bởi schema)
     * </ul>
     */
    private void recordPronunciationAnswer(
            PlacementTestSession session,
            Question question,
            String word,
            PronunciationScoreResult result) {

        boolean isCorrect = result.getOverallScore() != null
                && result.getOverallScore() >= PASSING_SCORE;

        PlacementTestAnswer answer = PlacementTestAnswer.builder()
                .session(session)
                .question(question)
                .answerGiven(word)
                .isCorrect(isCorrect)
                .timeSpentMs(null)       // Thời gian ghi âm không được client gửi lên
                .answeredAt(LocalDateTime.now())
                .build();

        answerRepository.save(answer);

        log.debug("Recorded pronunciation answer: sessionId={}, word='{}', isCorrect={}, score={}",
                session.getId(), word, isCorrect, result.getOverallScore());
    }

    // ─── Audio Bytes ──────────────────────────────────────────────────────────

    /**
     * Đọc bytes từ MultipartFile.
     *
     * @throws AppException AUDIO_PROCESSING_FAILED nếu file rỗng hoặc không đọc được.
     */
    private byte[] readAudioBytes(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw ErrorCode.AUDIO_PROCESSING_FAILED.toException();
        }
        try {
            return audioFile.getBytes();
        } catch (IOException e) {
            log.error("Failed to read audio file bytes", e);
            throw ErrorCode.AUDIO_PROCESSING_FAILED.toException();
        }
    }
}
