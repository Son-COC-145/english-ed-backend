package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.PlacementPronunciationAnswerResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
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

    public record PronunciationSubmissionPreparation(
            String referenceText,
            PlacementPronunciationAnswerResponse replayResponse) {
    }

    @Value("${onboarding.placement.max-questions:20}")
    private int maxPlacementQuestions;

    private static final Skill[] SKILL_BLUEPRINT = {
            Skill.VOCABULARY, Skill.GRAMMAR, Skill.READING, Skill.LISTENING, Skill.PRONUNCIATION
    };

    private final PlacementTestSessionRepository sessionRepository;
    private final PlacementTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final OnboardingRepository onboardingRepository;
    private final PlacementResultFactory resultFactory;
    private final PlacementQuestionContentMapper questionContentMapper;
    private final PlacementSessionExpiryService expiryService;
    private final UserRepository userRepository;
    private final RoadmapJobService roadmapJobService;

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
            // Session được tạo bởi phiên bản cũ không có per-skill state/issued question.
            // Không trộn hai thuật toán trong cùng một bài; đóng session cũ và bắt đầu lại.
            if (isLegacySession(session)) {
                log.warn("Closing legacy placement session {} for user {} before starting the 20-question flow",
                        session.getId(), userId);
                session.setIsCompleted(true);
                session.setCurrentQuestionId(null);
                sessionRepository.save(session);
                return startNewSession(userId);
            }
            if (session.isExpired()) {
                // Bài placement chỉ có giá trị khi đủ 20 câu. Session hết hạn được đóng như
                // một attempt bị bỏ dở, tuyệt đối không sinh kết quả từ dữ liệu một phần.
                log.warn("Session {} expired for user {} with {} answers. Starting a fresh attempt.",
                        session.getId(), userId, session.getCurrentQuestionIndex());
                session.setIsCompleted(true);
                session.setCurrentQuestionId(null);
                sessionRepository.save(session);
            } else {
                return getNextQuestion(session.getId(), userId);
            }
        }

        return startNewSession(userId);
    }

    public PlacementResultResponse skipTest(Long userId) {
        // Serialize repeated skip requests, then lock the active attempt so skip cannot race the
        // twentieth answer and overwrite a legitimately calculated result.
        com.example.english_app.entity.user.User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        sessionRepository.findActiveByStudentIdForUpdate(userId)
                .ifPresent(s -> {
                    s.setIsCompleted(true);
                    s.setCurrentQuestionId(null);
                });

        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder().student(user).build());

        if (onboarding.getPlacementCefrLevel() != null) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        // Đóng session đang dở (nếu có)
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
        queueRoadmapGeneration(onboarding, userId, CefrLevel.A1);

        log.info("Placement test skipped for user {}. Assigned default CEFR: A1 with baseline scores: {}", userId,
                baselineScore);

        // Roadmap được tạo sau commit để response skip/submit không phải chờ các query lộ trình
        // và tránh hai transaction cùng cập nhật một StudentOnboarding row.
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

        return resultFactory.buildResponse(CefrLevel.A1, skillCefrs, scores, Collections.emptyList(), null, false);
    }

    public PlacementQuestionResponse getNextQuestion(Long sessionId, Long userId) {
        rejectAndCommitIfExpired(sessionId, userId);
        PlacementTestSession session = requireSessionForUpdate(sessionId, userId);
        ensureSessionCanAcceptAnswer(session);

        int answeredCount = session.getCurrentQuestionIndex();
        if (answeredCount >= maxPlacementQuestions) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        // Idempotent: refresh/retry luôn nhận lại đúng câu đang chờ trả lời.
        if (session.getCurrentQuestionId() != null) {
            Question assigned = questionRepository.findById(session.getCurrentQuestionId())
                    .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());
            return buildQuestionResponse(sessionId, answeredCount, assigned);
        }

        Skill targetSkill = getTargetSkill(answeredCount);
        CefrLevel currentLevel = getSkillEstimate(session, targetSkill);
        Question next = pickNextQuestion(sessionId, targetSkill, currentLevel);
        session.setCurrentQuestionId(next.getId());
        sessionRepository.save(session);

        return buildQuestionResponse(sessionId, answeredCount, next);
    }

    public PlacementQuestionResponse submitAnswer(Long userId, PlacementAnswerRequest request) {
        if (request.getSubmissionId() == null) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        String requestHash = PlacementSubmissionHasher.answer(
                request.getSessionId(), request.getQuestionId(),
                request.getAnswerGiven(), request.getTimeSpentMs());

        rejectAndCommitIfExpired(request.getSessionId(), userId);
        PlacementTestSession session = requireSessionForUpdate(request.getSessionId(), userId);
        PlacementTestAnswer replay = findReplayCandidate(
                request.getSessionId(), request.getQuestionId(), request.getSubmissionId(),
                requestHash, "ANSWER");
        if (replay != null) {
            return replayAnswerResponse(session, userId, replay);
        }
        ensureSessionCanAcceptAnswer(session);

        Question question = requireAssignedQuestion(session, request.getQuestionId(), false);

        boolean isCorrect = question.getCorrectAnswer()
                .equalsIgnoreCase(request.getAnswerGiven() != null ? request.getAnswerGiven().trim() : "");

        Question next = advanceSessionAndAssignNext(session, question, isCorrect);

        answerRepository.save(PlacementTestAnswer.builder()
                .session(session)
                .question(question)
                .answerGiven(request.getAnswerGiven())
                .isCorrect(isCorrect)
                .timeSpentMs(request.getTimeSpentMs())
                .submissionId(request.getSubmissionId())
                .requestHash(requestHash)
                .submissionType("ANSWER")
                .answeredAt(LocalDateTime.now())
                .build());
        sessionRepository.save(session);

        int answeredCount = session.getCurrentQuestionIndex();
        if (answeredCount == maxPlacementQuestions) {
            // Bắt buộc flush answer thứ 20 trước khi guard count trong completeTestInternal.
            answerRepository.flush();
            PlacementResultResponse result = completeTestInternal(session, userId);
            return PlacementQuestionResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(request.getQuestionId())
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(result)
                    .previousAnswerCorrect(isCorrect)
                    .nextQuestion(null)
                    .build();
        }

        PlacementQuestionResponse response = buildQuestionResponse(session.getId(), answeredCount, next);
        response.setSubmittedQuestionId(request.getQuestionId());
        response.setSessionStatus("IN_PROGRESS");
        response.setPreviousAnswerCorrect(isCorrect);
        return response;
    }

    /**
     * Preflight nhẹ trước khi gọi dịch vụ chấm audio. Không giữ DB transaction trong lúc
     * chờ network; state sẽ được revalidate dưới row lock khi ghi kết quả.
     */
    @Transactional(readOnly = true)
    public PronunciationSubmissionPreparation preparePronunciationSubmission(
            Long userId,
            Long sessionId,
            Long questionId,
            UUID submissionId,
            String requestHash) {
        if (submissionId == null) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        PlacementTestSession session = requireSession(sessionId, userId);
        PlacementTestAnswer replay = findReplayCandidate(
                sessionId, questionId, submissionId, requestHash, "PRONUNCIATION");
        if (replay != null) {
            return new PronunciationSubmissionPreparation(
                    replay.getQuestion().getCorrectAnswer(),
                    replayPronunciationResponse(session, userId, replay));
        }
        rejectAndCommitIfExpired(sessionId, userId);
        if (Boolean.TRUE.equals(session.getIsCompleted())) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }
        if (session.isExpired()) {
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }
        String referenceText = requireAssignedQuestion(session, questionId, true).getCorrectAnswer();
        return new PronunciationSubmissionPreparation(referenceText, null);
    }

    /** Ghi kết quả pronunciation và tiến trình trong một transaction ngắn, có row lock. */
    public PlacementPronunciationAnswerResponse recordPronunciationAssessment(
            Long userId,
            Long sessionId,
            Long questionId,
            UUID submissionId,
            String requestHash,
            PronunciationScoreResult pronunciationResult) {

        if (submissionId == null || requestHash == null) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        if (pronunciationResult == null
                || !"SCORED".equals(pronunciationResult.getStatus())
                || pronunciationResult.getOverallScore() == null) {
            throw ErrorCode.PRONUNCIATION_UNAVAILABLE.toException();
        }

        rejectAndCommitIfExpired(sessionId, userId);
        PlacementTestSession session = requireSessionForUpdate(sessionId, userId);
        PlacementTestAnswer replay = findReplayCandidate(
                sessionId, questionId, submissionId, requestHash, "PRONUNCIATION");
        if (replay != null) {
            return replayPronunciationResponse(session, userId, replay);
        }
        ensureSessionCanAcceptAnswer(session);

        Question question = requireAssignedQuestion(session, questionId, true);
        boolean isCorrect = pronunciationResult.getOverallScore() != null
                && pronunciationResult.getOverallScore() >= 60;
        Question next = advanceSessionAndAssignNext(session, question, isCorrect);

        answerRepository.save(PlacementTestAnswer.builder()
                .session(session)
                .question(question)
                .answerGiven(question.getCorrectAnswer())
                .isCorrect(isCorrect)
                .timeSpentMs(null)
                .submissionId(submissionId)
                .requestHash(requestHash)
                .submissionType("PRONUNCIATION")
                .pronunciationOverallScore(pronunciationResult.getOverallScore())
                .pronunciationAccuracyScore(pronunciationResult.getAccuracyScore())
                .pronunciationFluencyScore(pronunciationResult.getFluencyScore())
                .pronunciationCompletenessScore(pronunciationResult.getCompletenessScore())
                .answeredAt(LocalDateTime.now())
                .build());
        sessionRepository.save(session);

        if (session.getCurrentQuestionIndex() == maxPlacementQuestions) {
            answerRepository.flush();
            PlacementResultResponse placementResult = completeTestInternal(session, userId);
            return PlacementPronunciationAnswerResponse.builder()
                    .sessionId(sessionId)
                    .submittedQuestionId(questionId)
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(placementResult)
                    .pronunciationResult(pronunciationResult)
                    .nextQuestion(null)
                    .build();
        }

        PlacementQuestionResponse nextResponse = buildQuestionResponse(
                sessionId, session.getCurrentQuestionIndex(), next);
        nextResponse.setSubmittedQuestionId(questionId);
        nextResponse.setPreviousAnswerCorrect(isCorrect);

        return PlacementPronunciationAnswerResponse.builder()
                .sessionId(sessionId)
                .submittedQuestionId(questionId)
                .sessionStatus("IN_PROGRESS")
                .isTestCompleted(false)
                .pronunciationResult(pronunciationResult)
                .nextQuestion(nextResponse)
                .build();
    }

    public PlacementResultResponse completeTest(Long sessionId, Long userId) {
        rejectAndCommitIfExpired(sessionId, userId);
        PlacementTestSession session = requireSessionForUpdate(sessionId, userId);

        if (session.getIsCompleted()) {
            return getResult(userId);
        }

        return completeTestInternal(session, userId);
    }

    private PlacementResultResponse completeTestInternal(PlacementTestSession session, Long userId) {
        List<PlacementTestAnswer> answers = answerRepository.findBySessionIdOrderByAnsweredAtAsc(session.getId());
        if (answers.size() != maxPlacementQuestions
                || session.getCurrentQuestionIndex() != maxPlacementQuestions) {
            log.warn("Rejected early placement completion: sessionId={}, persistedAnswers={}, index={}, expected={}",
                    session.getId(), answers.size(), session.getCurrentQuestionIndex(), maxPlacementQuestions);
            throw ErrorCode.PLACEMENT_TEST_INCOMPLETE.toException();
        }

        session.setIsCompleted(true);
        session.setCurrentQuestionId(null);
        sessionRepository.save(session);

        SkillScores scores = resultFactory.calculateAllSkills(answers);

        // Bound each estimate by the hardest level the learner actually saw. This prevents a
        // four-correct A2→B1→B2→C1 path from being reported as C2 without any C2 evidence.
        Map<Skill, CefrLevel> skillCefrs = getEvidenceBoundedSkillEstimates(session, answers);

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
        queueRoadmapGeneration(onboarding, userId, finalLevel);

        log.info("Placement test completed for user {}. Overall CEFR: {}, Skill CEFRs: {}",
                userId, finalLevel, skillCefrs);

        return resultFactory.buildResponse(finalLevel, skillCefrs, scores, answers, null, false);
    }

    /**
     * Persists the roadmap intent in the same transaction as the placement result. The worker may
     * run later, but a committed placement can no longer lose its roadmap request on process crash.
     */
    private void queueRoadmapGeneration(
            StudentOnboarding onboarding,
            Long userId,
            CefrLevel level) {
        int nextVersion = Optional.ofNullable(onboarding.getRoadmapGenerationVersion()).orElse(0) + 1;
        onboarding.setRoadmapJson(null);
        onboarding.setRoadmapStatus(com.example.english_app.entity.enums.RoadmapGenerationStatus.PENDING);
        onboarding.setRoadmapGenerationVersion(nextVersion);
        onboarding.setRoadmapGenerationAttempts(0);
        onboarding.setRoadmapLastError(null);
        onboarding.setRoadmapUpdatedAt(LocalDateTime.now());
        onboardingRepository.save(onboarding);
        roadmapJobService.enqueue(userId, nextVersion, level, onboarding.getGoalSurveyJson());
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

        List<PlacementTestAnswer> answers = Collections.emptyList();
        if (!Boolean.TRUE.equals(onboarding.getIsPlacementSkipped())) {
            PlacementTestSession session = sessionRepository
                    .findTopByStudentIdOrderByStartedAtDesc(userId)
                    .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());
            answers = answerRepository.findBySessionIdOrderByAnsweredAtAsc(session.getId());
        }

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

    private Map<Skill, CefrLevel> getEvidenceBoundedSkillEstimates(
            PlacementTestSession session,
            List<PlacementTestAnswer> answers) {
        Map<Skill, CefrLevel> estimates = getAllSkillEstimates(session);
        Map<Skill, CefrLevel> hardestSeen = new EnumMap<>(Skill.class);
        for (PlacementTestAnswer answer : answers) {
            if (answer.getQuestion() == null
                    || answer.getQuestion().getSkill() == null
                    || answer.getQuestion().getCefrLevel() == null) {
                continue;
            }
            hardestSeen.merge(
                    answer.getQuestion().getSkill(),
                    answer.getQuestion().getCefrLevel(),
                    (left, right) -> left.ordinal() >= right.ordinal() ? left : right);
        }
        for (Skill skill : SKILL_BLUEPRINT) {
            CefrLevel evidenceCeiling = hardestSeen.get(skill);
            CefrLevel estimate = estimates.get(skill);
            if (evidenceCeiling != null && estimate.ordinal() > evidenceCeiling.ordinal()) {
                estimates.put(skill, evidenceCeiling);
            }
        }
        return estimates;
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

        // Progress only; do not expose this value as statistical confidence.
        int answered = session.getCurrentQuestionIndex();
        double progress = (answered / (double) maxPlacementQuestions) * 100.0;
        session.setProgressPercent(BigDecimal.valueOf(Math.min(progress, 100.0)));
    }

    public int getMaxPlacementQuestions() {
        return maxPlacementQuestions;
    }

    /** Chọn câu gần target nhất bằng một DB round-trip, không load toàn bộ answered IDs. */
    private Question pickNextQuestion(Long sessionId, Skill skill, CefrLevel level) {
        return questionRepository.findBestAvailableForPlacement(sessionId, skill.name(), level.ordinal())
                .orElseThrow(() -> {
                    log.error("Exhausted all questions for skill {} in session {}", skill, sessionId);
                    return ErrorCode.PLACEMENT_QUESTION_EXHAUSTED.toException();
                });
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

    private PlacementTestAnswer findReplayCandidate(
            Long sessionId,
            Long questionId,
            UUID submissionId,
            String requestHash,
            String submissionType) {
        List<PlacementTestAnswer> candidates = answerRepository.findReplayCandidates(
                sessionId, submissionId, questionId);

        for (PlacementTestAnswer answer : candidates) {
            if (submissionId.equals(answer.getSubmissionId())) {
                if (!answer.getQuestion().getId().equals(questionId)
                        || !Objects.equals(answer.getRequestHash(), requestHash)
                        || !Objects.equals(answer.getSubmissionType(), submissionType)) {
                    throw ErrorCode.IDEMPOTENCY_KEY_REUSED.toException();
                }
                return answer;
            }
        }

        for (PlacementTestAnswer answer : candidates) {
            if (answer.getQuestion().getId().equals(questionId)) {
                if (Objects.equals(answer.getRequestHash(), requestHash)
                        && Objects.equals(answer.getSubmissionType(), submissionType)) {
                    return answer;
                }
                throw ErrorCode.ANSWER_ALREADY_SUBMITTED.toException();
            }
        }
        return null;
    }

    private PlacementQuestionResponse replayAnswerResponse(
            PlacementTestSession session,
            Long userId,
            PlacementTestAnswer answer) {
        if (Boolean.TRUE.equals(session.getIsCompleted())) {
            return PlacementQuestionResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(answer.getQuestion().getId())
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(getResult(userId))
                    .previousAnswerCorrect(answer.getIsCorrect())
                    .build();
        }

        Question next = questionRepository.findById(session.getCurrentQuestionId())
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());
        PlacementQuestionResponse response = buildQuestionResponse(
                session.getId(), session.getCurrentQuestionIndex(), next);
        response.setSubmittedQuestionId(answer.getQuestion().getId());
        response.setPreviousAnswerCorrect(answer.getIsCorrect());
        response.setPreviousCorrectAnswer(answer.getQuestion().getCorrectAnswer());
        return response;
    }

    private PlacementPronunciationAnswerResponse replayPronunciationResponse(
            PlacementTestSession session,
            Long userId,
            PlacementTestAnswer answer) {
        PronunciationScoreResult score = pronunciationScoreFrom(answer);
        if (Boolean.TRUE.equals(session.getIsCompleted())) {
            return PlacementPronunciationAnswerResponse.builder()
                    .sessionId(session.getId())
                    .submittedQuestionId(answer.getQuestion().getId())
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(getResult(userId))
                    .pronunciationResult(score)
                    .build();
        }

        Question next = questionRepository.findById(session.getCurrentQuestionId())
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());
        PlacementQuestionResponse nextResponse = buildQuestionResponse(
                session.getId(), session.getCurrentQuestionIndex(), next);
        nextResponse.setSubmittedQuestionId(answer.getQuestion().getId());
        nextResponse.setPreviousAnswerCorrect(answer.getIsCorrect());
        return PlacementPronunciationAnswerResponse.builder()
                .sessionId(session.getId())
                .submittedQuestionId(answer.getQuestion().getId())
                .sessionStatus("IN_PROGRESS")
                .isTestCompleted(false)
                .pronunciationResult(score)
                .nextQuestion(nextResponse)
                .build();
    }

    private PronunciationScoreResult pronunciationScoreFrom(PlacementTestAnswer answer) {
        short overall = answer.getPronunciationOverallScore();
        String color = overall >= 80 ? "GREEN" : overall >= 60 ? "YELLOW" : "RED";
        String level = overall >= 80 ? "EXCELLENT" : overall >= 60 ? "GOOD" : "NEEDS_PRACTICE";
        return PronunciationScoreResult.builder()
                .word(answer.getQuestion().getCorrectAnswer())
                .overallScore(overall)
                .accuracyScore(answer.getPronunciationAccuracyScore())
                .fluencyScore(answer.getPronunciationFluencyScore())
                .completenessScore(answer.getPronunciationCompletenessScore())
                .scoreColor(color)
                .scoreLevel(level)
                .status("SCORED")
                .build();
    }

    private PlacementTestSession requireSessionForUpdate(Long sessionId, Long userId) {
        PlacementTestSession session = sessionRepository.findByIdWithStudentForUpdate(sessionId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());
        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }
        return session;
    }

    private void ensureSessionCanAcceptAnswer(PlacementTestSession session) {
        if (Boolean.TRUE.equals(session.getIsCompleted())) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }
        if (session.isExpired()) {
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }
        if (session.getCurrentQuestionIndex() >= maxPlacementQuestions) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }
    }

    private void rejectAndCommitIfExpired(Long sessionId, Long userId) {
        if (expiryService.closeIfExpired(sessionId, userId)) {
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }
    }

    private Question requireAssignedQuestion(
            PlacementTestSession session,
            Long submittedQuestionId,
            boolean pronunciationEndpoint) {

        if (session.getCurrentQuestionId() == null
                || !session.getCurrentQuestionId().equals(submittedQuestionId)) {
            log.warn("Rejected unassigned question: sessionId={}, expected={}, submitted={}",
                    session.getId(), session.getCurrentQuestionId(), submittedQuestionId);
            throw ErrorCode.PLACEMENT_QUESTION_MISMATCH.toException();
        }

        Question question = questionRepository.findById(submittedQuestionId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());
        Skill expectedSkill = getTargetSkill(session.getCurrentQuestionIndex());
        boolean isPronunciation = question.getQuestionType() == QuestionType.PRONUNCIATION;

        if (question.getSkill() != expectedSkill || isPronunciation != pronunciationEndpoint) {
            log.warn("Rejected question with invalid blueprint/type: sessionId={}, questionId={}, "
                            + "expectedSkill={}, actualSkill={}, type={}",
                    session.getId(), question.getId(), expectedSkill, question.getSkill(), question.getQuestionType());
            throw ErrorCode.PLACEMENT_QUESTION_MISMATCH.toException();
        }
        return question;
    }

    /**
     * Cập nhật state trong memory và preselect câu tiếp theo trước khi persist answer.
     * Hai skill liền kề trong blueprint luôn khác nhau, nên câu hiện tại chưa flush
     * không thể bị query chọn lại cho bước kế tiếp.
     */
    private Question advanceSessionAndAssignNext(
            PlacementTestSession session,
            Question answeredQuestion,
            boolean isCorrect) {

        int answeredCount = session.getCurrentQuestionIndex() + 1;
        session.setCurrentQuestionIndex(answeredCount);
        session.setLastActivityAt(LocalDateTime.now());
        updateSkillCatState(session, answeredQuestion.getSkill(), isCorrect);
        session.setCurrentQuestionId(null);

        if (answeredCount == maxPlacementQuestions) {
            return null;
        }
        if (answeredCount > maxPlacementQuestions) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        Skill nextSkill = getTargetSkill(answeredCount);
        CefrLevel nextLevel = getSkillEstimate(session, nextSkill);
        Question next = pickNextQuestion(session.getId(), nextSkill, nextLevel);
        session.setCurrentQuestionId(next.getId());
        return next;
    }

    private boolean isLegacySession(PlacementTestSession session) {
        return session.getVocabCefrEstimate() == null
                && session.getGrammarCefrEstimate() == null
                && session.getReadingCefrEstimate() == null
                && session.getListeningCefrEstimate() == null
                && session.getPronunciationCefrEstimate() == null;
    }

    private void ensureQuestionBankCapacity() {
        Map<Skill, Long> activeBySkill = new EnumMap<>(Skill.class);
        for (Object[] row : questionRepository.countPlacementReadyGroupBySkill()) {
            activeBySkill.put((Skill) row[0], ((Number) row[1]).longValue());
        }

        int fullRounds = maxPlacementQuestions / SKILL_BLUEPRINT.length;
        int remainder = maxPlacementQuestions % SKILL_BLUEPRINT.length;
        for (int i = 0; i < SKILL_BLUEPRINT.length; i++) {
            Skill skill = SKILL_BLUEPRINT[i];
            long required = fullRounds + (i < remainder ? 1 : 0);
            long available = activeBySkill.getOrDefault(skill, 0L);
            if (available < required) {
                log.error("Insufficient active placement questions: skill={}, required={}, available={}",
                        skill, required, available);
                throw ErrorCode.PLACEMENT_QUESTION_EXHAUSTED.toException();
            }
        }
    }

    private PlacementQuestionResponse buildQuestionResponse(Long sessionId, int answeredCount, Question q) {
        Map<String, Object> content = questionContentMapper.toLearnerContent(q);

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
        // The user row is the aggregate lock for start/skip. Recheck state after acquiring it,
        // because another request may have created a session or completed placement meanwhile.
        com.example.english_app.entity.user.User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        Optional<StudentOnboarding> currentOnboarding = onboardingRepository.findByStudentId(userId);
        if (currentOnboarding.isPresent() && currentOnboarding.get().getPlacementCefrLevel() != null) {
            return PlacementQuestionResponse.builder()
                    .sessionStatus("COMPLETED")
                    .isTestCompleted(true)
                    .placementResult(getResult(userId))
                    .build();
        }

        Optional<PlacementTestSession> concurrentlyCreated =
                sessionRepository.findActiveByStudentIdForUpdate(userId);
        if (concurrentlyCreated.isPresent()) {
            return getNextQuestion(concurrentlyCreated.get().getId(), userId);
        }

        ensureQuestionBankCapacity();

        PlacementTestSession session = PlacementTestSession.builder()
                .student(user)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        session = sessionRepository.save(session);
        return getNextQuestion(session.getId(), userId);
    }
}
