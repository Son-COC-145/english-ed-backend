package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.gamification.DailyGoal;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.question.Question;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService {

    @Value("${onboarding.placement.max-questions:35}")
    private int maxPlacementQuestions;

    @Value("${onboarding.placement.confidence-threshold:85.0}")
    private double confidenceThreshold;

    @Value("${onboarding.placement.max-wrong-streak:3}")
    private int maxWrongStreak;

    private final UserRepository userRepository;
    private final OnboardingRepository onboardingRepository;
    private final PlacementTestSessionRepository sessionRepository;
    private final PlacementTestAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final DailyGoalRepository dailyGoalRepository;
    private final StudentStatRepository studentStatRepository;
    private final ObjectMapper objectMapper;
    private final RoadmapGenerationService roadmapGenerationService;

    // ONBOARDING STATUS

    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(Long userId) {
        User user = findUserById(userId);

        Optional<StudentOnboarding> optOnboarding = onboardingRepository.findByStudentId(user.getId());

        if (optOnboarding.isEmpty()) {
            return OnboardingStatusResponse.builder()
                    .goalSurveyCompleted(false)
                    .placementTestCompleted(false)
                    .settingsCompleted(false)
                    .onboardingCompleted(false)
                    .nextStep("GOAL_SURVEY")
                    .build();
        }

        StudentOnboarding onboarding = optOnboarding.get();
        boolean goalDone = onboarding.getGoalSurveyJson() != null;
        boolean placementDone = onboarding.getPlacementCefrLevel() != null;
        boolean settingsDone = onboarding.getDailyGoalXp() != null && onboarding.getDailyGoalXp() > 0;

        int stepNumber;
        String nextStep;
        if (!goalDone) {
            stepNumber = 2;
            nextStep = "GOAL_SURVEY";
        } else if (!placementDone) {
            stepNumber = 3;
            nextStep = "PLACEMENT_TEST";
        } else if (!settingsDone || !onboarding.getOnboardingCompleted()) {
            stepNumber = 6;
            nextStep = "SETTINGS";
        } else {
            stepNumber = 6;
            nextStep = "COMPLETED";
        }

        return OnboardingStatusResponse.builder()
                .goalSurveyCompleted(goalDone)
                .placementTestCompleted(placementDone)
                .settingsCompleted(settingsDone)
                .onboardingCompleted(onboarding.getOnboardingCompleted())
                .nextStep(nextStep)
                .stepNumber(stepNumber)
                .totalSteps(6)
                .userName(user.getFullName())
                .placementCefrLevel(placementDone ? onboarding.getPlacementCefrLevel().name() : null)
                .dailyGoalXp(onboarding.getDailyGoalXp())
                .roadmapGenerated(onboarding.getRoadmapJson() != null)
                .build();
    }

    // GOAL SURVEY

    public void submitGoalSurvey(Long userId, GoalSurveyRequest request) {
        User user = findUserById(userId);

        StudentOnboarding onboarding = onboardingRepository.findByStudentId(user.getId())
                .orElseGet(() -> StudentOnboarding.builder()
                        .student(user)
                        .build());

        try {
            onboarding.setGoalSurveyJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize goal survey", e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }

        onboardingRepository.save(onboarding);
    }

    // PLACEMENT TEST

    public PlacementQuestionResponse startPlacementTest(Long userId) {
        User user = findUserById(userId);

        // Guard clause: Nếu đã hoàn thành Placement Test rồi thì không cho làm lại
        onboardingRepository.findByStudentId(userId).ifPresent(ob -> {
            if (ob.getPlacementCefrLevel() != null) {
                throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
            }
        });

        // Kiểm tra session đang dở
        Optional<PlacementTestSession> existingSession =
                sessionRepository.findByStudentIdAndIsCompletedFalse(user.getId());

        if (existingSession.isPresent()) {
            PlacementTestSession existing = existingSession.get();

            // Kiểm tra Auto-save timeout: session cũ quá 30 phút -> hủy, tạo mới
            if (isSessionExpired(existing)) {
                log.warn("Placement test session {} expired for user {}. Starting new session.",
                        existing.getId(), userId);
                existing.setIsCompleted(true); // Đánh dấu là expired
                sessionRepository.save(existing);
            } else {
                // Session vẫn còn hạn -> tiếp tục
                return getNextQuestion(existing.getId(), userId);
            }
        }

        // Tạo phiên test mới
        PlacementTestSession session = PlacementTestSession.builder()
                .student(user)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .currentCefrEstimate(CefrLevel.A2)
                .build();

        session = sessionRepository.save(session);

        return getNextQuestion(session.getId(), userId);
    }

    public PlacementQuestionResponse getNextQuestion(Long sessionId, Long userId) {
        PlacementTestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        if (session.getIsCompleted()) {
            return null;
        }

        // Kiểm tra session timeout (30 phút không hoạt động)
        if (isSessionExpired(session)) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }

        int answeredCount = (int) answerRepository.countBySessionId(sessionId);

        if (answeredCount >= maxPlacementQuestions) {
            return null;
        }

        // Lấy danh sách question_id đã trả lời
        List<Long> answeredQuestionIds = answerRepository
                .findBySessionIdOrderByAnsweredAtAsc(sessionId)
                .stream()
                .map(a -> a.getQuestion().getId())
                .collect(Collectors.toList());

        // Adaptive: chọn câu hỏi theo level hiện tại
        CefrLevel currentLevel = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate()
                : CefrLevel.A2;

        Question nextQuestion = pickNextQuestion(currentLevel, answeredQuestionIds);

        if (nextQuestion == null) {
            return null; // Hết câu hỏi trong ngân hàng
        }

        // Parse content JSON
        Map<String, Object> content;
        try {
            content = objectMapper.readValue(nextQuestion.getContentJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse question content JSON for question {}", nextQuestion.getId(), e);
            content = Map.of("raw", nextQuestion.getContentJson());
        }

        return PlacementQuestionResponse.builder()
                .sessionId(sessionId)
                .questionId(nextQuestion.getId())
                .questionIndex(answeredCount + 1)
                .totalQuestions(maxPlacementQuestions)
                .cefrLevel(nextQuestion.getCefrLevel().name())
                .skill(nextQuestion.getSkill().name())
                .questionType(nextQuestion.getQuestionType().name())
                .timeoutSeconds(nextQuestion.getTimeoutSeconds())
                .content(content)
                .build();
    }

    public PlacementQuestionResponse submitAnswer(Long userId, PlacementAnswerRequest request) {
        PlacementTestSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        if (session.getIsCompleted()) {
            throw ErrorCode.PLACEMENT_TEST_ALREADY_COMPLETED.toException();
        }

        // Kiểm tra session timeout
        if (isSessionExpired(session)) {
            session.setIsCompleted(true);
            sessionRepository.save(session);
            throw ErrorCode.PLACEMENT_TEST_EXPIRED.toException();
        }

        // Kiểm tra câu hỏi đã được trả lời chưa (tránh double-submit)
        if (answerRepository.existsBySessionIdAndQuestionId(request.getSessionId(), request.getQuestionId())) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // Chấm điểm
        boolean isCorrect = question.getCorrectAnswer()
                .equalsIgnoreCase(request.getAnswerGiven() != null ? request.getAnswerGiven().trim() : "");

        PlacementTestAnswer answer = PlacementTestAnswer.builder()
                .session(session)
                .question(question)
                .answerGiven(request.getAnswerGiven())
                .isCorrect(isCorrect)
                .timeSpentMs(request.getTimeSpentMs())
                .answeredAt(LocalDateTime.now())
                .build();

        answerRepository.save(answer);

        // Cập nhật session
        session.setLastActivityAt(LocalDateTime.now());
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);

        // Cập nhật CEFR estimate dựa trên kết quả
        updateCefrEstimate(session, isCorrect);
        sessionRepository.save(session);

        int answeredCount = (int) answerRepository.countBySessionId(session.getId());

        // Tự động kết thúc nếu đủ câu hỏi hoặc confidence >= confidenceThreshold
        if (answeredCount >= maxPlacementQuestions ||
            (session.getConfidenceScore() != null && session.getConfidenceScore().doubleValue() >= confidenceThreshold)) {
            completePlacementTest(session.getId(), userId);
            return null;
        }

        // Trả câu hỏi tiếp theo
        return getNextQuestion(session.getId(), userId);
    }

    public PlacementResultResponse completePlacementTest(Long sessionId, Long userId) {
        PlacementTestSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        if (!session.getStudent().getId().equals(userId)) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        // Idempotent guard: nếu đã hoàn thành, trả về kết quả có sẵn
        if (session.getIsCompleted()) {
            return getPlacementResult(userId);
        }

        session.setIsCompleted(true);
        sessionRepository.save(session);

        // Tính điểm chi tiết từng kỹ năng
        List<PlacementTestAnswer> answers = answerRepository
                .findBySessionIdOrderByAnsweredAtAsc(sessionId);

        Map<Skill, List<PlacementTestAnswer>> bySkill = answers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getSkill()));

        short vocabScore = calculateSkillScore(bySkill.get(Skill.VOCABULARY));
        short grammarScore = calculateSkillScore(bySkill.get(Skill.GRAMMAR));
        short readingScore = calculateSkillScore(bySkill.get(Skill.READING));
        short listeningScore = calculateSkillScore(bySkill.get(Skill.LISTENING));
        short pronunciationScore = calculateSkillScore(bySkill.get(Skill.PRONUNCIATION));

        // CAT: Sử dụng CEFR level ước tính cuối cùng
        CefrLevel finalLevel = session.getCurrentCefrEstimate() != null
                ? session.getCurrentCefrEstimate() : CefrLevel.A1;

        // Lưu vào StudentOnboarding
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder()
                        .student(session.getStudent())
                        .build());

        onboarding.setPlacementCefrLevel(finalLevel);
        onboarding.setPlacementVocabScore(vocabScore);
        onboarding.setPlacementGrammarScore(grammarScore);
        onboarding.setPlacementReadingScore(readingScore);
        onboarding.setPlacementListeningScore(listeningScore);
        onboarding.setPlacementPronunciationScore(pronunciationScore);
        onboarding.setPlacementCompletedAt(LocalDateTime.now());

        onboardingRepository.save(onboarding);
        log.info("Placement test completed for user {}. CEFR level: {}", userId, finalLevel);
        
        List<String> suggestedModules = new ArrayList<>();
        boolean roadmapGenerated = false;
        try {
            RoadmapResponse roadmap = roadmapGenerationService.generateAndPersist(userId, finalLevel, onboarding.getGoalSurveyJson());
            if (roadmap != null && roadmap.getMilestones() != null && !roadmap.getMilestones().isEmpty()) {
                suggestedModules = roadmap.getMilestones().get(0).getModules().stream()
                        .map(RoadmapModule::getTitle)
                        .collect(Collectors.toList());
            }
            roadmapGenerated = true;
        } catch (Exception e) {
            log.error("Roadmap generation failed for user {}, but placement result is saved.", userId, e);
            suggestedModules = buildSuggestedModules(finalLevel);
        }

        Map<String, Short> skillScores = buildSkillScoreMap(vocabScore, grammarScore, readingScore, listeningScore, pronunciationScore);
        List<String> strengths = getTopSkills(skillScores, true);
        List<String> weaknesses = getTopSkills(skillScores, false);

        int totalCorrect = (int) answers.stream().filter(PlacementTestAnswer::getIsCorrect).count();

        return PlacementResultResponse.builder()
                .cefrLevel(finalLevel.name())
                .totalQuestions(answers.size())
                .correctAnswers(totalCorrect)
                .vocabScore(vocabScore)
                .grammarScore(grammarScore)
                .readingScore(readingScore)
                .listeningScore(listeningScore)
                .pronunciationScore(pronunciationScore)
                .radarChartData(skillScores)
                .message(buildResultMessage(finalLevel))
                .cefrDescription(buildCefrDescription(finalLevel))
                .strengths(strengths)
                .weaknesses(weaknesses)
                .roadmapGenerated(roadmapGenerated)
                .suggestedModules(suggestedModules)
                .build();
    }

    @Transactional(readOnly = true)
    public PlacementResultResponse getPlacementResult(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        if (onboarding.getPlacementCefrLevel() == null) {
            throw ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException();
        }

        PlacementTestSession session = sessionRepository
                .findTopByStudentIdOrderByStartedAtDesc(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());

        List<PlacementTestAnswer> answers = answerRepository
                .findBySessionIdOrderByAnsweredAtAsc(session.getId());

        int totalCorrect = (int) answers.stream().filter(PlacementTestAnswer::getIsCorrect).count();

        Map<String, Short> skillScores = buildSkillScoreMap(
                onboarding.getPlacementVocabScore(),
                onboarding.getPlacementGrammarScore(),
                onboarding.getPlacementReadingScore(),
                onboarding.getPlacementListeningScore(),
                onboarding.getPlacementPronunciationScore());

        List<String> strengths = getTopSkills(skillScores, true);
        List<String> weaknesses = getTopSkills(skillScores, false);
        
        List<String> suggestedModules = new ArrayList<>();
        if (onboarding.getRoadmapJson() != null) {
            try {
                RoadmapResponse roadmap = objectMapper.readValue(onboarding.getRoadmapJson(), RoadmapResponse.class);
                if (roadmap.getMilestones() != null && !roadmap.getMilestones().isEmpty()) {
                    suggestedModules = roadmap.getMilestones().get(0).getModules().stream()
                            .map(RoadmapModule::getTitle)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Failed to parse roadmap_json for user {}", userId);
                suggestedModules = buildSuggestedModules(onboarding.getPlacementCefrLevel());
            }
        } else {
            suggestedModules = buildSuggestedModules(onboarding.getPlacementCefrLevel());
        }

        return PlacementResultResponse.builder()
                .cefrLevel(onboarding.getPlacementCefrLevel().name())
                .totalQuestions(answers.size())
                .correctAnswers(totalCorrect)
                .vocabScore(onboarding.getPlacementVocabScore())
                .grammarScore(onboarding.getPlacementGrammarScore())
                .readingScore(onboarding.getPlacementReadingScore())
                .listeningScore(onboarding.getPlacementListeningScore())
                .pronunciationScore(onboarding.getPlacementPronunciationScore())
                .radarChartData(skillScores)
                .message(buildResultMessage(onboarding.getPlacementCefrLevel()))
                .cefrDescription(buildCefrDescription(onboarding.getPlacementCefrLevel()))
                .strengths(strengths)
                .weaknesses(weaknesses)
                .roadmapGenerated(onboarding.getRoadmapJson() != null)
                .suggestedModules(suggestedModules)
                .build();
    }
    
    public RoadmapResponse getRoadmap(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());
                
        if (onboarding.getRoadmapJson() == null) {
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }
        
        try {
            return objectMapper.readValue(onboarding.getRoadmapJson(), RoadmapResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse roadmap_json for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    // SETTINGS

    public void saveSettings(Long userId, OnboardingSettingsRequest request) {
        User user = findUserById(userId);

        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // Validate dailyGoalXp chỉ nhận 10, 20, 30, 50
        if (!request.isValidDailyGoalXp()) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        onboarding.setDailyGoalXp(request.getDailyGoalXp());

        if (request.getReminderTime() != null) {
            onboarding.setReminderTime(request.getReminderTime());
        }

        onboardingRepository.save(onboarding);

        // Khởi tạo DailyGoal cho ngày hôm nay (nếu chưa có)
        LocalDate today = LocalDate.now();
        if (!dailyGoalRepository.existsByStudentIdAndGoalDate(userId, today)) {
            DailyGoal dailyGoal = DailyGoal.builder()
                    .student(user)
                    .goalDate(today)
                    .targetXp(request.getDailyGoalXp())
                    .build();
            dailyGoalRepository.save(dailyGoal);
            log.info("Created initial DailyGoal for user {} with targetXp={}", userId, request.getDailyGoalXp());
        }

        // Khởi tạo StudentStat (nếu chưa có)
        if (!studentStatRepository.existsById(userId)) {
            StudentStat stat = StudentStat.builder()
                    .student(user)
                    .build();
            studentStatRepository.save(stat);
            log.info("Initialized StudentStat for user {}", userId);
        }
    }

    public void completeOnboarding(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // Guard clause: chống gọi lại nhiều lần
        if (onboarding.getOnboardingCompleted()) {
            throw ErrorCode.ONBOARDING_ALREADY_COMPLETED.toException();
        }

        // Pre-condition: phải hoàn thành Goal Survey, Placement Test và Settings trước
        boolean goalDone = onboarding.getGoalSurveyJson() != null;
        boolean placementDone = onboarding.getPlacementCefrLevel() != null;
        boolean settingsDone = onboarding.getDailyGoalXp() != null && onboarding.getDailyGoalXp() > 0;

        if (!goalDone || !placementDone || !settingsDone) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        onboarding.setOnboardingCompleted(true);
        onboarding.setOnboardingCompletedAt(LocalDateTime.now());

        onboardingRepository.save(onboarding);
        log.info("Onboarding completed for user {}", userId);
    }

    //  PRIVATE HELPERS

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    /**
     * Kiểm tra session có hết hạn 30 phút không.
     * Nếu lastActivityAt cách hiện tại > 30 phút → expired.
     */
    private boolean isSessionExpired(PlacementTestSession session) {
        if (session.getLastActivityAt() == null) return false;
        return session.getLastActivityAt().isBefore(LocalDateTime.now().minusMinutes(30));
    }

    /**
     * Xây dựng Map điểm kỹ năng dùng cho Radar Chart và Top Skills.
     * Tránh lặp code giữa completePlacementTest() và getPlacementResult().
     */
    private Map<String, Short> buildSkillScoreMap(short vocab, short grammar, short reading, short listening, short pronunciation) {
        Map<String, Short> map = new LinkedHashMap<>();
        map.put("Từ vựng", vocab);
        map.put("Ngữ pháp", grammar);
        map.put("Đọc hiểu", reading);
        map.put("Nghe", listening);
        map.put("Phát âm", pronunciation);
        return map;
    }

    /**
     * Chọn câu hỏi tiếp theo theo chiến lược adaptive:
     * Ưu tiên lấy câu hỏi ở level hiện tại, nếu hết thì lấy level trên/dưới.
     */
    private Question pickNextQuestion(CefrLevel currentLevel, List<Long> excludeIds) {
        if (excludeIds.isEmpty()) {
            excludeIds = List.of(-1L); // Dummy để tránh lỗi SQL IN ()
        }

        List<Question> candidates = questionRepository
                .findRandomByCefrLevelExcluding(currentLevel, excludeIds);

        if (!candidates.isEmpty()) {
            return candidates.get(0);
        }

        // Thử level kế tiếp
        CefrLevel[] levels = CefrLevel.values();
        int currentIndex = currentLevel.ordinal();

        // Tìm level trên
        if (currentIndex + 1 < levels.length) {
            candidates = questionRepository
                    .findRandomByCefrLevelExcluding(levels[currentIndex + 1], excludeIds);
            if (!candidates.isEmpty()) return candidates.get(0);
        }

        // Tìm level dưới
        if (currentIndex - 1 >= 0) {
            candidates = questionRepository
                    .findRandomByCefrLevelExcluding(levels[currentIndex - 1], excludeIds);
            if (!candidates.isEmpty()) return candidates.get(0);
        }

        return null;
    }

    /**
     * Cập nhật ước tính CEFR dựa trên kết quả của câu vừa trả lời (CAT)
     * Trả lời đúng: tăng 1 bậc, trả lời sai: giảm 1 bậc
     * Tính confidence score. Dừng sớm nếu sai 3 lần liên tiếp.
     */
    private void updateCefrEstimate(PlacementTestSession session, boolean isLastCorrect) {
        CefrLevel[] levels = CefrLevel.values();
        int currentIndex = session.getCurrentCefrEstimate().ordinal();

        if (isLastCorrect) {
            if (currentIndex < levels.length - 1) {
                session.setCurrentCefrEstimate(levels[currentIndex + 1]);
            }
        } else {
            if (currentIndex > 0) {
                session.setCurrentCefrEstimate(levels[currentIndex - 1]);
            }
        }

        List<PlacementTestAnswer> answers = answerRepository
                .findBySessionIdOrderByAnsweredAtAsc(session.getId());

        int answeredCount = answers.size();
        
        // Đếm số lần sai liên tiếp
        int wrongStreak = 0;
        for (int i = answeredCount - 1; i >= 0; i--) {
            if (!answers.get(i).getIsCorrect()) {
                wrongStreak++;
            } else {
                break;
            }
        }

        double confidence = (answeredCount / (double) maxPlacementQuestions) * 100.0;
        
        // Dừng sớm nếu sai liên tiếp vượt quá maxWrongStreak
        if (wrongStreak >= maxWrongStreak) {
            confidence = 100.0;
        }

        session.setConfidenceScore(java.math.BigDecimal.valueOf(Math.min(confidence, 100.0)));
    }


    private short calculateSkillScore(List<PlacementTestAnswer> answers) {
        if (answers == null || answers.isEmpty()) return 0;

        long correct = answers.stream().filter(PlacementTestAnswer::getIsCorrect).count();
        return (short) Math.round((double) correct / answers.size() * 100);
    }

    private String buildResultMessage(CefrLevel level) {
        return switch (level) {
            case A1 -> "Bạn đang ở trình độ Sơ cấp (A1). Hãy bắt đầu với những từ vựng và cấu trúc cơ bản nhất!";
            case A2 -> "Bạn đang ở trình độ Sơ cấp (A2). Bạn đã nắm được căn bản, hãy củng cố thêm!";
            case B1 -> "Bạn đang ở trình độ Trung cấp (B1). Bạn có thể giao tiếp trong các tình huống quen thuộc!";
            case B2 -> "Bạn đang ở trình độ Trung cấp cao (B2). Hãy thử thách với các chủ đề phức tạp hơn!";
            case C1 -> "Xuất sắc! Bạn đang ở trình độ Cao cấp (C1). Hãy hoàn thiện kỹ năng nâng cao!";
        };
    }

    private String buildCefrDescription(CefrLevel level) {
        return switch (level) {
            case A1, A2 -> "Mới bắt đầu, vốn từ < 500 từ";
            case B1 -> "Giao tiếp đơn giản, hiểu ngữ cảnh quen thuộc";
            case B2 -> "Tự tin giao tiếp hầu hết tình huống thường gặp";
            case C1 -> "Thành thạo, xử lý được nội dung phức tạp";
        };
    }

    private List<String> buildSuggestedModules(CefrLevel level) {
        return switch (level) {
            case A1, A2 -> List.of("Từ vựng cơ bản", "Phát âm IPA nền", "Speaking cơ bản");
            case B1 -> List.of("Từ vựng chủ đề", "Speaking tình huống", "Phát âm âm đuôi");
            case B2 -> List.of("Speaking nâng cao", "Từ vựng học thuật", "Phát âm ngữ điệu");
            case C1 -> List.of("Speaking đàm phán / thuyết trình", "Luyện thi chứng chỉ");
        };
    }

    private List<String> getTopSkills(Map<String, Short> scores, boolean getStrengths) {
        List<Map.Entry<String, Short>> sortedScores = new ArrayList<>(scores.entrySet());
        if (getStrengths) {
            sortedScores.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        } else {
            sortedScores.sort(Map.Entry.comparingByValue());
        }

        return sortedScores.stream()
                .filter(e -> getStrengths ? e.getValue() > 0 : true) // avoid 0 score as strength
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
