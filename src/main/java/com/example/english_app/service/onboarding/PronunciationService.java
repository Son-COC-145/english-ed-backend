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
 * <p><b>SRP:</b> Class này chỉ biết về Pronunciation trong ngữ cảnh Placement Test.
 * Nó KHÔNG biết về CAT algorithm, goal survey, hay settings — những thứ đó
 * thuộc về {@link OnboardingService}.
 *
 * <p><b>Dependency flow (một chiều):</b>
 * {@code OnboardingController} → {@code PronunciationService} → {@code AudioAssessmentPort}
 * {@code OnboardingController} → {@code OnboardingService}
 * {@code OnboardingService} KHÔNG phụ thuộc vào {@code PronunciationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationService {

    private static final int PASSING_SCORE = 60; // Điểm tối thiểu để tính isCorrect = true

    private final AudioAssessmentPort audioAssessmentPort;
    private final PlacementTestSessionRepository sessionRepository;
    private final PlacementTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Entry point từ controller: nhận audio, đánh giá phát âm, ghi kết quả vào DB.
     *
     * <p>Flow:
     * <ol>
     *   <li>Validate session (ownership + trạng thái + timeout).
     *   <li>Đọc audio bytes từ MultipartFile.
     *   <li>Gọi {@link AudioAssessmentPort#assess} (có fallback nội bộ).
     *   <li>Lưu {@link PlacementTestAnswer} với skill = PRONUNCIATION.
     *   <li>Cập nhật lastActivityAt.
     *   <li>Trả về kết quả cho controller.
     * </ol>
     *
     * @param userId    ID user lấy từ JWT.
     * @param sessionId ID session đang làm bài.
     * @param questionId ID câu hỏi đang làm
     * @param audioFile File audio từ multipart/form-data.
     * @param word      Từ tham chiếu cần phát âm.
     * @param wordIndex Vị trí của từ trong bộ câu hỏi phát âm (dùng để log).
     * @return Kết quả đánh giá phát âm.
     */
    @Transactional
    public PronunciationScoreResult submitPronunciation(
            Long userId,
            Long sessionId,
            Long questionId,
            MultipartFile audioFile,
            String word,
            int wordIndex) {

        // 1. Validate session
        PlacementTestSession session = validateSessionForPronunciation(sessionId, userId);

        // Fetch question
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // 2. Đọc audio bytes
        byte[] audioBytes = readAudioBytes(audioFile);

        // 3. Gọi Azure (fallback nội bộ trong AudioAssessmentPort — không bao giờ throw)
        PronunciationScoreResult result = audioAssessmentPort.assess(audioBytes, word);

        log.info("Pronunciation assessed: userId={}, sessionId={}, word='{}', wordIndex={}, status={}, score={}",
                userId, sessionId, word, wordIndex, result.getStatus(), result.getOverallScore());

        // 4. Ghi kết quả vào PlacementTestAnswer
        recordPronunciationAnswer(session, question, word, result);

        // 5. Cập nhật lastActivityAt
        session.setLastActivityAt(LocalDateTime.now());
        sessionRepository.save(session);

        return result;
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

        // Guard: timeout (30 phút không hoạt động)
        if (isSessionExpired(session)) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }

        return session;
    }

    /**
     * Kiểm tra session có hết hạn chưa (> 30 phút không hoạt động).
     * Mirror logic của OnboardingService để giữ nhất quán mà không tạo coupling.
     */
    private boolean isSessionExpired(PlacementTestSession session) {
        if (session.getLastActivityAt() == null) return false;
        return session.getLastActivityAt().isBefore(LocalDateTime.now().minusMinutes(30));
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
     *   <li>question = null (pronunciation không dùng question từ ngân hàng câu hỏi)
     * </ul>
     *
     * <p>Note: Trường {@code question} được set null vì pronunciation assessment
     * không lấy câu hỏi từ bảng {@code questions}.
     * Đây là use-case hợp lệ — nullable FK được cho phép bởi schema.
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
