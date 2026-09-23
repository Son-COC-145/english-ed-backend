package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
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

    @Value("${onboarding.placement.max-questions:20}")
    private int maxPlacementQuestions;

    @Value("${onboarding.placement.max-wrong-streak:3}")
    private int maxWrongStreak;

    private static final Skill[] SKILL_BLUEPRINT = {
            Skill.VOCABULARY, Skill.GRAMMAR, Skill.READING, Skill.LISTENING, Skill.PRONUNCIATION
    };

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
        onboarding.setPlacementVocabCefr(CefrLevel.A1);
        onboarding.setPlacementGrammarCefr(CefrLevel.A1);
        onboarding.setPlacementReadingCefr(CefrLevel.A1);
        onboarding.setPlacementListeningCefr(CefrLevel.A1);
        onboarding.setPlacementPronunciationCefr(CefrLevel.A1);
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

        Map<Skill, CefrLevel> skillCefrs = new EnumMap<>(Skill.class);
        for (Skill s : SKILL_BLUEPRINT) {
            skillCefrs.put(s, CefrLevel.A1);
        }

        return resultFactory.buildResponse(CefrLevel.A1, skillCefrs, scores, Collections.emptyList(), roadmapJson,
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

        Skill targetSkill = getTargetSkill(answeredCount);
        CefrLevel currentLevel = getSkillEstimate(session, targetSkill);

        Question next = pickNextQuestion(targetSkill, currentLevel, answeredIds);

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
        updateSkillCatState(session, question.getSkill(), isCorrect);
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
        List<Long> answeredIds = answerRepository.findAnsweredQuestionIdsBySessionId(session.getId());
        if (answeredIds.isEmpty()) {
            answeredIds = List.of(-1L);
        }

        Skill nextSkill = getTargetSkill(answeredCount);
        CefrLevel nextLevel = getSkillEstimate(session, nextSkill);

        Question next = pickNextQuestion(nextSkill, nextLevel, answeredIds);

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

        // 5 per-skill CEFR estimates
        Map<Skill, CefrLevel> skillCefrs = getAllSkillEstimates(session);

        // Overall CEFR = median của 5 skill estimates
        CefrLevel finalLevel = resultFactory.calculateFinalCefrMedian(skillCefrs);

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

        // 5 per-skill CEFR columns
        onboarding.setPlacementVocabCefr(skillCefrs.get(Skill.VOCABULARY));
        onboarding.setPlacementGrammarCefr(skillCefrs.get(Skill.GRAMMAR));
        onboarding.setPlacementReadingCefr(skillCefrs.get(Skill.READING));
        onboarding.setPlacementListeningCefr(skillCefrs.get(Skill.LISTENING));
        onboarding.setPlacementPronunciationCefr(skillCefrs.get(Skill.PRONUNCIATION));

        onboarding.setPlacementCompletedAt(LocalDateTime.now());
        onboardingRepository.save(onboarding);

        log.info("Placement test completed for user {}. Overall CEFR: {}, Skill CEFRs: {}",
                userId, finalLevel, skillCefrs);

        // Sinh lộ trình — dùng try-catch riêng để lỗi roadmap không rollback kết quả placement.
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

        return resultFactory.buildResponse(finalLevel, skillCefrs, scores, answers, roadmapJson, roadmapGenerated);
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

        Map<Skill, CefrLevel> skillCefrs = new EnumMap<>(Skill.class);
        skillCefrs.put(Skill.VOCABULARY, onboarding.getPlacementVocabCefr() != null
                ? onboarding.getPlacementVocabCefr() : onboarding.getPlacementCefrLevel());
        skillCefrs.put(Skill.GRAMMAR, onboarding.getPlacementGrammarCefr() != null
                ? onboarding.getPlacementGrammarCefr() : onboarding.getPlacementCefrLevel());
        skillCefrs.put(Skill.READING, onboarding.getPlacementReadingCefr() != null
                ? onboarding.getPlacementReadingCefr() : onboarding.getPlacementCefrLevel());
        skillCefrs.put(Skill.LISTENING, onboarding.getPlacementListeningCefr() != null
                ? onboarding.getPlacementListeningCefr() : onboarding.getPlacementCefrLevel());
        skillCefrs.put(Skill.PRONUNCIATION, onboarding.getPlacementPronunciationCefr() != null
                ? onboarding.getPlacementPronunciationCefr() : onboarding.getPlacementCefrLevel());

        return resultFactory.buildResponse(
                onboarding.getPlacementCefrLevel(),
                skillCefrs,
                scores,
                answers,
                onboarding.getRoadmapJson(),
                onboarding.getRoadmapJson() != null);
    }

    // ─── Blueprint & Skill Helpers ────────────────────────────────────────────

    /**
     * Xác định skill cho câu hỏi tiếp theo (dựa trên answeredCount).
     * Blueprint cố định: 5 skills xoay vòng round-robin.
     */
    public Skill getTargetSkill(int answeredCount) {
        return SKILL_BLUEPRINT[answeredCount % SKILL_BLUEPRINT.length];
    }

    /**
     * Lấy CEFR estimate hiện tại cho một skill cụ thể trong session.
     */
    public CefrLevel getSkillEstimate(PlacementTestSession session, Skill skill) {
        CefrLevel estimate = switch (skill) {
            case VOCABULARY -> session.getVocabCefrEstimate();
            case GRAMMAR -> session.getGrammarCefrEstimate();
            case READING -> session.getReadingCefrEstimate();
            case LISTENING -> session.getListeningCefrEstimate();
            case PRONUNCIATION -> session.getPronunciationCefrEstimate();
        };
        return estimate != null ? estimate : CefrLevel.A2;
    }

    /**
     * Cập nhật CEFR estimate cho một skill cụ thể trong session.
     */
    public void setSkillEstimate(PlacementTestSession session, Skill skill, CefrLevel level) {
        switch (skill) {
            case VOCABULARY -> session.setVocabCefrEstimate(level);
            case GRAMMAR -> session.setGrammarCefrEstimate(level);
            case READING -> session.setReadingCefrEstimate(level);
            case LISTENING -> session.setListeningCefrEstimate(level);
            case PRONUNCIATION -> session.setPronunciationCefrEstimate(level);
        }
    }

    /**
     * Lấy tất cả 5 per-skill CEFR estimates từ session.
     */
    public Map<Skill, CefrLevel> getAllSkillEstimates(PlacementTestSession session) {
        Map<Skill, CefrLevel> map = new EnumMap<>(Skill.class);
        for (Skill s : SKILL_BLUEPRINT) {
            map.put(s, getSkillEstimate(session, s));
        }
        return map;
    }

    /**
     * Cập nhật CEFR estimate độc lập cho từng skill.
     * Đúng: +1 bậc (tối đa C2)
     * Sai: -1 bậc (tối thiểu A1)
     *
     * <p><b>Disclaimer:</b> CEFR level được tính từ placement test này là heuristic ước lượng
     * cho mục tiêu phân loại nhanh trong app học tiếng Anh. Đây không phải chứng nhận CEFR chính thức.
     */
    public void updateSkillCatState(PlacementTestSession session, Skill skill, boolean isCorrect) {
        CefrLevel[] levels = CefrLevel.values();
        CefrLevel current = getSkillEstimate(session, skill);
        int idx = current.ordinal();

        if (isCorrect) {
            if (idx < levels.length - 1) {
                setSkillEstimate(session, skill, levels[idx + 1]);
            }
            session.setCurrentWrongStreak(0);
        } else {
            if (idx > 0) {
                setSkillEstimate(session, skill, levels[idx - 1]);
            }
            session.setCurrentWrongStreak(session.getCurrentWrongStreak() + 1);
        }

        // confidence_score dùng làm chỉ số tiến độ (progress %)
        int answered = session.getCurrentQuestionIndex();
        double progress = (answered / (double) maxPlacementQuestions) * 100.0;
        session.setConfidenceScore(BigDecimal.valueOf(Math.min(progress, 100.0)));
    }

    public int getMaxPlacementQuestions() {
        return maxPlacementQuestions;
    }

    /**
     * Chọn câu hỏi tiếp theo cho skill chỉ định theo chiến lược Nearest-Level Fallback:
     * 1. Exact: level yêu cầu + cùng skill
     * 2. level - 1 (nếu có)
     * 3. level + 1 (nếu có)
     * 4. Sweep theo khoảng cách tăng dần: level - 2, level + 2, level - 3, level + 3... cùng skill
     * 5. Hết câu hỏi cho skill này -> ném PLACEMENT_QUESTION_EXHAUSTED (tuyệt đối KHÔNG đổi skill)
     */
    private Question pickNextQuestion(Skill skill, CefrLevel level, List<Long> excludeIds) {
        CefrLevel[] levels = CefrLevel.values();
        int idx = level.ordinal();

        // 1. Thử level hiện tại
        Optional<Question> exact = questionRepository.findOneRandomByLevelAndSkillExcluding(
                level.name(), skill.name(), excludeIds);
        if (exact.isPresent()) {
            return exact.get();
        }

        // 2, 3, 4. Sweep theo khoảng cách tăng dần (ưu tiên level thấp hơn trước: idx - d, rồi idx + d)
        for (int d = 1; d < levels.length; d++) {
            if (idx - d >= 0) {
                Optional<Question> lower = questionRepository.findOneRandomByLevelAndSkillExcluding(
                        levels[idx - d].name(), skill.name(), excludeIds);
                if (lower.isPresent()) {
                    log.info("Nearest-level fallback for skill {}: requested {}, found {}",
                            skill, level, levels[idx - d]);
                    return lower.get();
                }
            }
            if (idx + d < levels.length) {
                Optional<Question> higher = questionRepository.findOneRandomByLevelAndSkillExcluding(
                        levels[idx + d].name(), skill.name(), excludeIds);
                if (higher.isPresent()) {
                    log.info("Nearest-level fallback for skill {}: requested {}, found {}",
                            skill, level, levels[idx + d]);
                    return higher.get();
                }
            }
        }

        // 5. Hết câu hỏi cho skill này -> tuyệt đối KHÔNG đổi skill khác
        log.error("Exhausted all questions for skill {} with excludeIds size {}", skill, excludeIds.size());
        throw ErrorCode.PLACEMENT_QUESTION_EXHAUSTED.toException();
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
                .build();

        session = sessionRepository.save(session);
        return getNextQuestion(session.getId(), userId);
    }
}
