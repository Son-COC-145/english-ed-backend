package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.question.Question;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.onboarding.PlacementResultFactory.SkillScores;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PlacementTestService {

    @Value("${onboarding.placement.max-questions:15}")
    private int maxPlacementQuestions;

    @Value("${onboarding.placement.max-wrong-streak:3}")
    private int maxWrongStreak;

    private final PlacementTestSessionRepository sessionRepository;
    private final PlacementTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final OnboardingRepository onboardingRepository;
    private final RoadmapGenerationService roadmapGenerationService;
    private final PlacementResultFactory resultFactory;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public PlacementQuestionResponse startTest(Long userId) {
        // Guard: đã hoàn thành placement test rồi → KHÔNG throw error,
        // trả về completion signal để FE tự động điều hướng đến màn hình kết quả.
        // Trước đây throw exception → FE hiển thị lỗi "đã hoàn thành" gây nhầm lẫn khi
        // mở lại app sau khi CAT kết thúc sớm (early-stop) mà user chưa kịp thấy kết quả.
        var existingOnboarding = onboardingRepository.findByStudentId(userId);
        if (existingOnboarding.isPresent() && existingOnboarding.get().getPlacementCefrLevel() != null) {
            PlacementResultResponse result = getResult(userId);
            return PlacementQuestionResponse.builder()
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(result)
                    .nextQuestion(null)
                    .build();
        }

        // Kiểm tra session đang dở
        Optional<PlacementTestSession> existing = sessionRepository
                .findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(userId);

        if (existing.isPresent()) {
            PlacementTestSession session = existing.get();
            if (session.isExpired()) {
                // P1-C fix: Nếu user đã trả lời ít nhất 1 câu, tự động hoàn thành
                // (dùng CAT state đã tính) để giữ điểm số — thay vì silently drop toàn bộ tiến trình.
                // Nếu session rỗng (không có câu trả lời nào), chỉ đóng và tạo session mới.
                if (session.getCurrentQuestionIndex() > 0) {
                    log.warn("Session {} expired for user {} with {} answers. Auto-completing to preserve grade.",
                            session.getId(), userId, session.getCurrentQuestionIndex());
                    try {
                        PlacementResultResponse result = completeTest(session.getId(), userId);
                        return PlacementQuestionResponse.builder()
                                .sessionStatus("COMPLETED")
                                .isTestCompleted(true)
                                .placementResult(result)
                                .nextQuestion(null)
                                .build();
                    } catch (Exception e) {
                        log.error("Auto-complete of expired session {} failed; starting new session.", session.getId(), e);
                        session.setIsCompleted(true);
                        sessionRepository.save(session);
                    }
                } else {
                    log.warn("Session {} expired for user {} with no answers. Creating new session.", session.getId(), userId);
                    session.setIsCompleted(true);
                    sessionRepository.save(session);
                }
            } else {
                return getNextQuestion(session.getId(), userId);
            }
        }

        return startNewSession(userId);
    }

    public PlacementResultResponse skipTest(Long userId) {
        com.example.english_app.entity.user.User user = userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder().student(user).build());

        if (onboarding.getPlacementCefrLevel() != null) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        // Đóng session đang dở (nếu có)
        sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(userId)
                .ifPresent(s -> {
                    s.setIsCompleted(true);
                    sessionRepository.save(s);
                });

        short baselineScore = 20;
        onboarding.setPlacementCefrLevel(CefrLevel.A1);
        onboarding.setIsPlacementSkipped(true);
        onboarding.setPlacementVocabScore(baselineScore);
        onboarding.setPlacementGrammarScore(baselineScore);
        onboarding.setPlacementReadingScore(baselineScore);
        onboarding.setPlacementListeningScore(baselineScore);
        onboarding.setPlacementPronunciationScore(baselineScore);
        onboarding.setPlacementCompletedAt(LocalDateTime.now());
        onboardingRepository.save(onboarding);

        log.info("Placement test skipped for user {}. Assigned default CEFR: A1 with baseline scores: {}", userId,
                baselineScore);

        // Tự động sinh Roadmap.
        // Lưu ý: generateAndPersist() chạy trong transaction REQUIRES_NEW và TỰ PERSIST roadmap_json
        // vào DB rồi. Chúng ta chỉ cần đọc roadmapJson từ kết quả trả về để build response;
        // KHÔNG cần gọi onboardingRepository.save() lần nữa — tránh ghi đè bằng object stale.
        String roadmapJson = null;
        boolean roadmapGenerated = false;
        try {
            var roadmap = roadmapGenerationService.generateAndPersist(userId, CefrLevel.A1,
                    onboarding.getGoalSurveyJson());
            if (roadmap != null) {
                roadmapJson = objectMapper.writeValueAsString(roadmap);
                roadmapGenerated = true;
            }
        } catch (Exception e) {
            log.error("Roadmap generation failed during skipTest for user {}", userId, e);
        }

        SkillScores scores = SkillScores.builder()
                .vocab(baselineScore)
                .grammar(baselineScore)
                .reading(baselineScore)
                .listening(baselineScore)
                .pronunciation(baselineScore)
                .build();

        return resultFactory.buildResponse(CefrLevel.A1, scores, Collections.emptyList(), roadmapJson,
                roadmapGenerated);
    }

    public PlacementQuestionResponse getNextQuestion(Long sessionId, Long userId) {
        PlacementTestSession session = requireSession(sessionId, userId);

        if (session.getIsCompleted()) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }
        if (session.isExpired()) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }

        int answeredCount = session.getCurrentQuestionIndex();

        if (answeredCount >= maxPlacementQuestions) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        List<Long> answeredIds = answerRepository.findAnsweredQuestionIdsBySessionId(sessionId);
        if (answeredIds.isEmpty()) {
            answeredIds = List.of(-1L);
        }

        CefrLevel currentLevel = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate()
                : CefrLevel.A2;

        Question next = pickNextQuestion(currentLevel, answeredIds);
        if (next == null) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        return buildQuestionResponse(sessionId, answeredCount, next);
    }

    public PlacementQuestionResponse submitAnswer(Long userId, PlacementAnswerRequest request) {
        PlacementTestSession session = requireSession(request.getSessionId(), userId);

        if (session.getIsCompleted()) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }
        if (session.isExpired()) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }
        if (answerRepository.existsBySessionIdAndQuestionId(request.getSessionId(), request.getQuestionId())) {
            log.warn("Duplicate answer submit: sessionId={}, questionId={}, userId={}",
                    request.getSessionId(), request.getQuestionId(), userId);
            throw ErrorCode.ANSWER_ALREADY_SUBMITTED.toException();
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        boolean isCorrect = question.getCorrectAnswer()
                .equalsIgnoreCase(request.getAnswerGiven() != null ? request.getAnswerGiven().trim() : "");

        answerRepository.save(PlacementTestAnswer.builder()
                .session(session)
                .question(question)
                .answerGiven(request.getAnswerGiven())
                .isCorrect(isCorrect)
                .timeSpentMs(request.getTimeSpentMs())
                .answeredAt(LocalDateTime.now())
                .build());

        session.setLastActivityAt(LocalDateTime.now());
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        updateCatState(session, isCorrect);
        sessionRepository.save(session);

        int answeredCount = session.getCurrentQuestionIndex();

        // Kết thúc khi đã làm đủ maxPlacementQuestions câu (không còn early-stop).
        // Đảm bảo tất cả user đều trải qua đủ số câu để đánh giá chính xác.
        if (answeredCount >= maxPlacementQuestions) {
            PlacementResultResponse result = completeTest(session.getId(), userId);
            return PlacementQuestionResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(request.getQuestionId())
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(result)
                    .previousAnswerCorrect(isCorrect)
                    .previousCorrectAnswer(question.getCorrectAnswer())
                    .nextQuestion(null)
                    .build();
        }

        // Tối ưu: thay vì gọi getNextQuestion() (tốn thêm 1 query findAnsweredQuestionIdsBySessionId),
        // ta dùng answered IDs từ session + câu vừa trả lời để pick trực tiếp.
        // Tiết kiệm 1 DB round-trip mỗi lần submit → giảm latency đáng kể.
        List<Long> answeredIds = answerRepository.findAnsweredQuestionIdsBySessionId(session.getId());
        if (answeredIds.isEmpty()) {
            answeredIds = List.of(-1L);
        }

        CefrLevel nextLevel = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate() : CefrLevel.A2;

        Question next = pickNextQuestion(nextLevel, answeredIds);
        if (next == null) {
            // Hết câu hỏi khả dụng — kết thúc sớm do data shortage (không phải CAT)
            log.warn("No more questions available for session {} after {} answers. Completing test.",
                    session.getId(), answeredCount);
            PlacementResultResponse result = completeTest(session.getId(), userId);
            return PlacementQuestionResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(request.getQuestionId())
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(result)
                    .previousAnswerCorrect(isCorrect)
                    .previousCorrectAnswer(question.getCorrectAnswer())
                    .nextQuestion(null)
                    .build();
        }

        PlacementQuestionResponse response = buildQuestionResponse(session.getId(), answeredCount, next);
        response.setSubmittedQuestionId(request.getQuestionId());
        response.setSessionStatus("IN_PROGRESS");
        response.setPreviousAnswerCorrect(isCorrect);
        response.setPreviousCorrectAnswer(question.getCorrectAnswer());
        return response;
    }

    public PlacementResultResponse completeTest(Long sessionId, Long userId) {
        PlacementTestSession session = requireSession(sessionId, userId);

        if (session.getIsCompleted()) {
            return getResult(userId);
        }

        session.setIsCompleted(true);
        sessionRepository.save(session);

        List<PlacementTestAnswer> answers = answerRepository.findBySessionIdOrderByAnsweredAtAsc(sessionId);
        SkillScores scores = resultFactory.calculateAllSkills(answers);

        CefrLevel finalLevel = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate()
                : CefrLevel.A1;

        // Lưu kết quả vào StudentOnboarding
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder()
                        .student(session.getStudent())
                        .build());

        onboarding.setPlacementCefrLevel(finalLevel);
        onboarding.setIsPlacementSkipped(false);
        onboarding.setPlacementVocabScore(scores.getVocab());
        onboarding.setPlacementGrammarScore(scores.getGrammar());
        onboarding.setPlacementReadingScore(scores.getReading());
        onboarding.setPlacementListeningScore(scores.getListening());
        onboarding.setPlacementPronunciationScore(scores.getPronunciation());
        onboarding.setPlacementCompletedAt(LocalDateTime.now());
        onboardingRepository.save(onboarding);

        log.info("Placement test completed for user {}. CEFR: {}", userId, finalLevel);

        // Sinh lộ trình — dùng try-catch riêng để lỗi roadmap không rollback kết quả placement.
        // generateAndPersist() chạy trong transaction REQUIRES_NEW và TỰ PERSIST roadmap_json;
        // chỉ cần serialize kết quả trả về để build response — KHÔNG save lại onboarding ở đây.
        String roadmapJson = null;
        boolean roadmapGenerated = false;
        try {
            var roadmap = roadmapGenerationService.generateAndPersist(userId, finalLevel,
                    onboarding.getGoalSurveyJson());
            if (roadmap != null) {
                roadmapJson = objectMapper.writeValueAsString(roadmap);
                roadmapGenerated = true;
            }
        } catch (Exception e) {
            log.error("Roadmap generation failed for user {}, placement result still saved.", userId, e);
        }

        return resultFactory.buildResponse(finalLevel, scores, answers, roadmapJson, roadmapGenerated);
    }

    /**
     * Đọc kết quả bài test đã làm trước đó (không tính lại).
     */
    @Transactional(readOnly = true)
    public PlacementResultResponse getResult(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        if (onboarding.getPlacementCefrLevel() == null) {
            throw ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException();
        }

        PlacementTestSession session = sessionRepository
                .findTopByStudentIdOrderByStartedAtDesc(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        List<PlacementTestAnswer> answers = answerRepository.findBySessionIdOrderByAnsweredAtAsc(session.getId());

        SkillScores scores = SkillScores.builder()
                .vocab(onboarding.getPlacementVocabScore())
                .grammar(onboarding.getPlacementGrammarScore())
                .reading(onboarding.getPlacementReadingScore())
                .listening(onboarding.getPlacementListeningScore())
                .pronunciation(onboarding.getPlacementPronunciationScore())
                .build();

        return resultFactory.buildResponse(
                onboarding.getPlacementCefrLevel(),
                scores,
                answers,
                onboarding.getRoadmapJson(),
                onboarding.getRoadmapJson() != null);
    }

    // CAT Algorithm
    public void updateCatState(PlacementTestSession session, boolean isCorrect) {
        CefrLevel[] levels = CefrLevel.values();
        // Defensive: fallback về A2 nếu currentCefrEstimate null (session cũ hoặc edge case)
        CefrLevel current = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate() : CefrLevel.A2;
        int idx = current.ordinal();

        if (isCorrect) {
            if (idx < levels.length - 1)
                session.setCurrentCefrEstimate(levels[idx + 1]);
            session.setCurrentWrongStreak(0);
        } else {
            if (idx > 0)
                session.setCurrentCefrEstimate(levels[idx - 1]);
            session.setCurrentWrongStreak(session.getCurrentWrongStreak() + 1);
        }

        // confidence_score dùng làm chỉ số tiến độ (progress %) thay vì trigger early-stop.
        int answered = session.getCurrentQuestionIndex();
        double progress = (answered / (double) maxPlacementQuestions) * 100.0;
        session.setConfidenceScore(BigDecimal.valueOf(Math.min(progress, 100.0)));
    }

    public int getMaxPlacementQuestions() {
        return maxPlacementQuestions;
    }

    private Question pickNextQuestion(CefrLevel level, List<Long> excludeIds) {
        CefrLevel[] levels = CefrLevel.values();
        int idx = level.ordinal();

        return questionRepository.findOneRandomByCefrLevelExcluding(level.name(), excludeIds)
                .or(() -> idx + 1 < levels.length
                        ? questionRepository.findOneRandomByCefrLevelExcluding(levels[idx + 1].name(), excludeIds)
                        : Optional.empty())
                .or(() -> idx - 1 >= 0
                        ? questionRepository.findOneRandomByCefrLevelExcluding(levels[idx - 1].name(), excludeIds)
                        : Optional.empty())
                .orElse(null);
    }

    // Private Helpers
    private PlacementTestSession requireSession(Long sessionId, Long userId) {
        // JOIN FETCH student để tránh N+1: student.getId() check ngay bên dưới
        // không cần thêm SELECT nữa.
        PlacementTestSession session = sessionRepository.findByIdWithStudent(sessionId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());
        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }
        return session;
    }

    private PlacementQuestionResponse buildQuestionResponse(Long sessionId, int answeredCount, Question q) {
        Map<String, Object> content;
        try {
            content = objectMapper.readValue(q.getContentJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to parse question content JSON for question {}", q.getId(), e);
            content = Map.of("raw", q.getContentJson());
        }

        // Section 3.6: Chuẩn hóa metadata cho câu hỏi phát âm
        if (q.getQuestionType() != null && q.getQuestionType().name().equals("PRONUNCIATION")) {
            Map<String, Object> enriched = new LinkedHashMap<>(content);
            if (!enriched.containsKey("ipaTranscription") && enriched.containsKey("ipa")) {
                enriched.put("ipaTranscription", enriched.get("ipa"));
            }
            if (!enriched.containsKey("instruction")) {
                enriched.put("instruction", "Đọc to từ bên dưới vào microphone");
            }
            content = enriched;
        }

        // Tạo nested nextQuestion Map để Mobile dễ mapping theo hợp đồng { "nextQuestion": { ... } }
        Map<String, Object> nextMap = new LinkedHashMap<>();
        nextMap.put("questionId", q.getId());
        nextMap.put("questionIndex", answeredCount + 1);
        nextMap.put("totalQuestions", maxPlacementQuestions);
        nextMap.put("cefrLevel", q.getCefrLevel().name());
        nextMap.put("skill", q.getSkill().name());
        nextMap.put("questionType", q.getQuestionType().name());
        nextMap.put("timeoutSeconds", q.getTimeoutSeconds());
        nextMap.put("content", content);

        return PlacementQuestionResponse.builder()
                .sessionId(sessionId)
                .sessionStatus("IN_PROGRESS")
                .questionId(q.getId())
                .questionIndex(answeredCount + 1)
                .totalQuestions(maxPlacementQuestions)
                .cefrLevel(q.getCefrLevel().name())
                .skill(q.getSkill().name())
                .questionType(q.getQuestionType().name())
                .timeoutSeconds(q.getTimeoutSeconds())
                .content(content)
                .nextQuestion(nextMap)
                .build();
    }

    // Tạo session mới — tách riêng để dễ unit test
    private PlacementQuestionResponse startNewSession(Long userId) {
        com.example.english_app.entity.user.User user = userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        PlacementTestSession session = PlacementTestSession.builder()
                .student(user)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .currentCefrEstimate(CefrLevel.A2)
                .build();

        session = sessionRepository.save(session);
        return getNextQuestion(session.getId(), userId);
    }
}
