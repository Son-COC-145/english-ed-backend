package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.RoadmapGenerationStatusResponse;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.gamification.DailyGoal;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.DailyGoalRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.adaptive.roadmap.RoadmapProgressService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Quản lý vòng đời (lifecycle) của quá trình Onboarding.
 *
 * <p><b>Trách nhiệm:</b>
 * <ul>
 *   <li>Trạng thái và điều hướng từng bước (step status / next step)
 *   <li>Goal Survey (lưu mục tiêu)
 *   <li>Settings (daily goal XP, reminder)
 *   <li>Complete / Reset onboarding
 *   <li>Lấy lộ trình (roadmap) từ JSON đã lưu
 * </ul>
 *
 * <p><b>Dependency graph (không vòng):</b>
 * {@code OnboardingController} → {@code OnboardingLifecycleService}  (lifecycle only)
 * {@code OnboardingController} → {@code PlacementTestService}         (test only)
 * {@code OnboardingLifecycleService} KHÔNG phụ thuộc vào {@code PlacementTestService}
 *   hoặc {@code PlacementResultFactory}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingLifecycleService {

    private final UserRepository              userRepository;
    private final OnboardingRepository        onboardingRepository;
    private final PlacementTestSessionRepository sessionRepository;
    private final DailyGoalRepository         dailyGoalRepository;
    private final StudentStatRepository       studentStatRepository;
    private final ObjectMapper                objectMapper;
    private final RoadmapJobService           roadmapJobService;
    private final RoadmapProgressService      roadmapProgressService;

    // ─── Step constants (dùng chung với frontend) ────────────────────────────
    // Flow: GOAL_SURVEY(1) → PLACEMENT_TEST(2) → ROADMAP_VIEW(3) → SETTINGS(4) → COMPLETED(5)
    private static final int STEP_GOAL_SURVEY    = 1;
    private static final int STEP_PLACEMENT_TEST = 2;
    private static final int STEP_ROADMAP_VIEW   = 3;  // Xem lộ trình tự động sau khi làm xong bài test
    private static final int STEP_SETTINGS       = 4;
    private static final int STEP_COMPLETED      = 5;
    private static final int TOTAL_STEPS         = 5;

    // ─── Status ───────────────────────────────────────────────────────────────

    /**
     * Tính toán và trả về trạng thái onboarding hiện tại của user.
     *
     * <p><b>Flow chuẩn:</b> GOAL_SURVEY (1) → PLACEMENT_TEST (2) → ROADMAP_VIEW (3)
     * → SETTINGS/COMPLETE (4) → COMPLETED (5)
     *
     * <p><b>Invariant (FE contract):</b>
     * <ul>
     *   <li>Goal survey chưa xong (và placement chưa xong)     → nextStep = GOAL_SURVEY
     *   <li>Goal xong, placement IN_PROGRESS                   → nextStep = PLACEMENT_TEST
     *   <li>Goal xong, placement chưa xử lý                   → nextStep = PLACEMENT_TEST
     *   <li>Placement xong, roadmap chưa sẵn sàng              → nextStep = ROADMAP_GENERATING/ROADMAP_FAILED
     *   <li>Goal + Placement đã xong, Settings chưa xong       → nextStep = SETTINGS
     *   <li>Goal + Placement + Settings xong, chưa complete     → nextStep = COMPLETE
     *   <li>Onboarding completed                               → nextStep = COMPLETED (Terminal)
     * </ul>
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        Optional<StudentOnboarding> opt = onboardingRepository.findByStudentIdWithUser(userId);

        if (opt.isEmpty()) {
            User user = findUser(userId);
            return OnboardingStatusResponse.builder()
                    .goalSurveyCompleted(false)
                    .placementTestCompleted(false)
                    .isPlacementSkipped(false)
                    .settingsCompleted(false)
                    .onboardingCompleted(false)
                    .placementTestStatus("NOT_STARTED")
                    .activePlacementSessionId(null)
                    .nextStep("GOAL_SURVEY")
                    .stepNumber(STEP_GOAL_SURVEY)
                    .totalSteps(TOTAL_STEPS)
                    .userName(user.getFullName())
                    .roadmapGenerated(false)
                    .roadmapStatus("NOT_STARTED")
                    .build();
        }

        StudentOnboarding ob = opt.get();
        String userName       = ob.getStudent().getFullName();
        boolean goalDone      = ob.getGoalSurveyJson() != null;
        boolean placementDone = ob.getPlacementCefrLevel() != null;
        boolean roadmapDone   = isRoadmapReady(ob);
        RoadmapGenerationStatus roadmapStatus = effectiveRoadmapStatus(ob);
        boolean settingsDone  = ob.getDailyGoalXp() != null && ob.getDailyGoalXp() > 0;
        boolean isCompleted   = Boolean.TRUE.equals(ob.getOnboardingCompleted());

        // 1. Phân loại trạng thái Placement Test rõ ràng (Section 3.2)
        String placementStatus = "NOT_STARTED";
        Long activeSessionId = null;

        if (placementDone) {
            if (Boolean.TRUE.equals(ob.getIsPlacementSkipped())) {
                placementStatus = "SKIPPED";
            } else {
                placementStatus = "COMPLETED";
            }
        } else if (goalDone) {
            var activeSessionOpt = sessionRepository
                    .findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(userId);
            if (activeSessionOpt.isPresent()) {
                var s = activeSessionOpt.get();
                if (!s.isExpired()) {
                    placementStatus = "IN_PROGRESS";
                    activeSessionId = s.getId();
                }
            }
        }

        // 2. Xác định nextStep tuân thủ tuyệt đối bảng Invariant (Section 3.1 & 3.2)
        String nextStep;
        int stepNumber;

        if (!goalDone) {
            nextStep = "GOAL_SURVEY";
            stepNumber = STEP_GOAL_SURVEY;
        } else if ("IN_PROGRESS".equals(placementStatus)) {
            nextStep = "PLACEMENT_TEST";
            stepNumber = STEP_PLACEMENT_TEST;
        } else if (!placementDone) {
            nextStep = "PLACEMENT_TEST";
            stepNumber = STEP_PLACEMENT_TEST;
        } else if (!roadmapDone) {
            nextStep = roadmapStatus == RoadmapGenerationStatus.FAILED
                    ? "ROADMAP_FAILED"
                    : "ROADMAP_GENERATING";
            stepNumber = STEP_ROADMAP_VIEW;
        } else if (!settingsDone) {
            nextStep = "SETTINGS";
            stepNumber = STEP_SETTINGS;
        } else if (!isCompleted) {
            // ĐÃ XONG CẢ 3 BƯỚC NHƯNG CHƯA BẤM COMPLETE -> BƯỚC TIẾP LÀ COMPLETE (KHÔNG BAO GIỜ LÀ SETTINGS)
            nextStep = "COMPLETE";
            stepNumber = STEP_SETTINGS;
        } else {
            nextStep = "COMPLETED";
            stepNumber = STEP_COMPLETED;
        }

        return OnboardingStatusResponse.builder()
                .goalSurveyCompleted(goalDone)
                .placementTestCompleted(placementDone)
                .isPlacementSkipped(Boolean.TRUE.equals(ob.getIsPlacementSkipped()))
                .settingsCompleted(settingsDone)
                .onboardingCompleted(isCompleted)
                .placementTestStatus(placementStatus)
                .activePlacementSessionId(activeSessionId)
                .nextStep(nextStep)
                .stepNumber(stepNumber)
                .totalSteps(TOTAL_STEPS)
                .userName(userName)
                .placementCefrLevel(placementDone ? ob.getPlacementCefrLevel().name() : null)
                .dailyGoalXp(ob.getDailyGoalXp())
                .roadmapGenerated(roadmapDone)
                .roadmapStatus(roadmapStatus != null ? roadmapStatus.name() : "NOT_STARTED")
                .roadmapRetryAfterMs(isRoadmapInProgress(roadmapStatus) ? 1500L : null)
                .build();
    }

    // ─── Goal Survey ──────────────────────────────────────────────────────────

    /** Lưu kết quả khảo sát mục tiêu. Tạo StudentOnboarding record nếu chưa có. */
    public void submitGoalSurvey(Long userId, GoalSurveyRequest request) {
        // Serialize concurrent survey retries/changes with placement start/skip for this learner.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder().student(user).build());

        final String goalSurveyJson;
        try {
            goalSurveyJson = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize goal survey for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }

        // Network retries must stay idempotent, including after placement/onboarding completion.
        if (sameJson(ob.getGoalSurveyJson(), goalSurveyJson)) {
            return;
        }
        if (Boolean.TRUE.equals(ob.getOnboardingCompleted())) {
            throw ErrorCode.ONBOARDING_ALREADY_COMPLETED.toException();
        }

        boolean firstGoalAfterLegacyPlacement = isBlank(ob.getGoalSurveyJson())
                && ob.getPlacementCefrLevel() != null;
        if (!isBlank(ob.getGoalSurveyJson()) && hasPlacementStarted(userId, ob)) {
            throw ErrorCode.GOAL_SURVEY_LOCKED.toException();
        }

        ob.setGoalSurveyJson(goalSurveyJson);
        if (firstGoalAfterLegacyPlacement) {
            queueRoadmapRegeneration(ob, userId);
            return;
        }
        onboardingRepository.save(ob);
    }

    // ─── Roadmap ──────────────────────────────────────────────────────────────

    /**
     * Đọc lộ trình đã được sinh ra và lưu trong StudentOnboarding.
     * Lộ trình được tạo bởi RoadmapGenerationService (trong PlacementTestService).
     * Đây chỉ là đọc — không sinh lại.
     */
    @Transactional
    public RoadmapResponse getRoadmap(Long userId) {
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        if (!isRoadmapReady(ob)) {
            if (effectiveRoadmapStatus(ob) == RoadmapGenerationStatus.FAILED) {
                throw ErrorCode.ROADMAP_GENERATION_FAILED.toException();
            }
            if (ob.getPlacementCefrLevel() != null) {
                throw ErrorCode.ROADMAP_GENERATING.toException();
            }
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }

        try {
            return roadmapProgressService.recalculateAll(ob).roadmap();
        } catch (IllegalStateException e) {
            log.error("Failed to resolve roadmap for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    @Transactional(readOnly = true)
    public RoadmapGenerationStatusResponse getRoadmapGenerationStatus(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());
        RoadmapGenerationStatus status = effectiveRoadmapStatus(onboarding);
        boolean ready = isRoadmapReady(onboarding);
        return RoadmapGenerationStatusResponse.builder()
                .status(status != null ? status.name() : "NOT_STARTED")
                .ready(ready)
                .attempts(Optional.ofNullable(onboarding.getRoadmapGenerationAttempts()).orElse(0))
                .retryAfterMs(isRoadmapInProgress(status) ? 1500L : null)
                .build();
    }

    public RoadmapGenerationStatusResponse retryRoadmap(Long userId) {
        roadmapJobService.retry(userId);
        return getRoadmapGenerationStatus(userId);
    }

    /**
     * Lấy tiến độ học tập trên lộ trình (Roadmap Progress).
     */
    @Transactional
    public RoadmapProgressResponse getRoadmapProgress(Long userId) {
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        if (ob.getRoadmapJson() == null) {
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }

        try {
            return roadmapProgressService.recalculateAll(ob).progress();
        } catch (IllegalStateException e) {
            log.error("Failed to calculate roadmap progress for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    /** Lưu cài đặt cá nhân hoá và khởi tạo DailyGoal + StudentStat nếu chưa có. */
    public void saveSettings(Long userId, OnboardingSettingsRequest request) {
        if (!request.isValidDailyGoalXp()) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        // Serialize settings with /complete so a late request cannot skip or reopen onboarding.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        requireStudent(user);
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.GOAL_SURVEY_REQUIRED.toException());

        if (Boolean.TRUE.equals(ob.getOnboardingCompleted())) {
            boolean sameSettings = Objects.equals(ob.getDailyGoalXp(), request.getDailyGoalXp())
                    && (request.getReminderTime() == null
                    || Objects.equals(ob.getReminderTime(), request.getReminderTime()));
            if (sameSettings) return;
            throw ErrorCode.ONBOARDING_ALREADY_COMPLETED.toException();
        }
        if (isBlank(ob.getGoalSurveyJson())) {
            throw ErrorCode.GOAL_SURVEY_REQUIRED.toException();
        }
        if (ob.getPlacementCefrLevel() == null) {
            throw ErrorCode.PLACEMENT_TEST_INCOMPLETE.toException();
        }
        if (!isRoadmapReady(ob)) {
            if (effectiveRoadmapStatus(ob) == RoadmapGenerationStatus.FAILED) {
                throw ErrorCode.ROADMAP_GENERATION_FAILED.toException();
            }
            throw ErrorCode.ROADMAP_GENERATING.toException();
        }

        ob.setDailyGoalXp(request.getDailyGoalXp());
        if (request.getReminderTime() != null) {
            ob.setReminderTime(request.getReminderTime());
        }
        onboardingRepository.save(ob);

        // Khởi tạo DailyGoal cho ngày hôm nay (nếu chưa có)
        LocalDate today = LocalDate.now();
        if (!dailyGoalRepository.existsByStudentIdAndGoalDate(userId, today)) {
            dailyGoalRepository.save(DailyGoal.builder()
                    .student(user)
                    .goalDate(today)
                    .targetXp(request.getDailyGoalXp())
                    .build());
            log.info("Created DailyGoal for user {} with targetXp={}", userId, request.getDailyGoalXp());
        }

        // Khởi tạo StudentStat (nếu chưa có)
        if (!studentStatRepository.existsById(userId)) {
            studentStatRepository.save(StudentStat.builder().student(user).build());
            log.info("Initialized StudentStat for user {}", userId);
        }
    }

    // ─── Complete / Reset ─────────────────────────────────────────────────────

    /**
     * Đánh dấu hoàn tất toàn bộ onboarding.
     * Pre-condition: goalSurvey + placementTest + settings đều phải xong.
     */
    public void completeOnboarding(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        requireStudent(user);
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        // Mobile may retry after the first 200 response was lost. Treat completion as a terminal,
        // idempotent command and repair the read-optimized users flag if necessary.
        if (Boolean.TRUE.equals(ob.getOnboardingCompleted())) {
            if (!Boolean.TRUE.equals(user.getOnboardingCompleted())) {
                user.setOnboardingCompleted(true);
                userRepository.save(user);
            }
            return;
        }

        // Pre-condition: tất cả 3 bước (goal, placement, settings) phải hoàn tất.
        // roadmapJson cũng phải tồn tại — nếu không, Mobile sẽ vào home mà không có lộ trình.
        boolean allDone = ob.getGoalSurveyJson() != null
                && ob.getPlacementCefrLevel() != null
                && ob.getDailyGoalXp() != null && ob.getDailyGoalXp() > 0
                && isRoadmapReady(ob);

        if (!allDone) {
            log.warn("completeOnboarding() called with incomplete state for user {}: " +
                            "goalSurvey={}, placement={}, dailyGoalXp={}, roadmap={}",
                    userId,
                    ob.getGoalSurveyJson() != null,
                    ob.getPlacementCefrLevel() != null,
                    ob.getDailyGoalXp(),
                    ob.getRoadmapJson() != null);
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        ob.setOnboardingCompleted(true);
        ob.setOnboardingCompletedAt(LocalDateTime.now());
        user.setOnboardingCompleted(true);
        onboardingRepository.save(ob);
        userRepository.save(user);
        log.info("Onboarding completed for user {}", userId);
    }

    /**
     * Reset toàn bộ kết quả onboarding và placement test (chỉ ADMIN được gọi — guard ở Controller).
     * Đảm bảo tất cả fields — kể cả dailyGoalXp và reminderTime — được reset về giá trị mặc định
     * để tránh getStatus() trả settingsDone=true sau khi reset.
     */
    public void resetOnboarding(Long userId) {
        // Serialize reset with start/skip and lock the active session so an in-flight final answer
        // cannot republish placement state after the reset.
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        requireStudent(user);
        Optional<PlacementTestSession> activeSession =
                sessionRepository.findActiveByStudentIdForUpdate(userId);

        onboardingRepository.findByStudentId(userId).ifPresent(ob -> {
            ob.setGoalSurveyJson(null);
            ob.setPlacementCefrLevel(null);
            ob.setPlacementVocabScore(null);
            ob.setPlacementGrammarScore(null);
            ob.setPlacementReadingScore(null);
            ob.setPlacementListeningScore(null);
            ob.setPlacementPronunciationScore(null);
            ob.setPlacementVocabCefr(null);
            ob.setPlacementGrammarCefr(null);
            ob.setPlacementReadingCefr(null);
            ob.setPlacementListeningCefr(null);
            ob.setPlacementPronunciationCefr(null);
            ob.setPlacementCompletedAt(null);
            ob.setIsPlacementSkipped(false);
            ob.setOnboardingCompleted(false);
            ob.setOnboardingCompletedAt(null);
            ob.setRoadmapJson(null);
            ob.setRoadmapStatus(null);
            ob.setRoadmapGenerationVersion(
                    Optional.ofNullable(ob.getRoadmapGenerationVersion()).orElse(0) + 1);
            ob.setRoadmapGenerationAttempts(0);
            ob.setRoadmapLastError(null);
            ob.setRoadmapUpdatedAt(null);
            // Reset settings — quan trọng: nếu không reset, getStatus() sẽ thấy
            // settingsDone=true (dailyGoalXp vẫn còn giá trị cũ) và bỏ qua bước Settings.
            ob.setDailyGoalXp(null);
            ob.setReminderTime(null);
            onboardingRepository.save(ob);
            log.info("Onboarding reset for user {}", userId);
        });

        user.setOnboardingCompleted(false);
        userRepository.save(user);

        activeSession.ifPresent(s -> {
            s.setIsCompleted(true);
            s.setCurrentQuestionId(null);
            sessionRepository.save(s);
        });
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    private void requireStudent(User user) {
        if (!Role.STUDENT.equals(user.getRole())) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }

    private boolean hasPlacementStarted(Long userId, StudentOnboarding onboarding) {
        if (onboarding.getPlacementCefrLevel() != null) return true;
        return sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(userId)
                .filter(session -> !session.isExpired())
                .isPresent();
    }

    private boolean sameJson(String currentJson, String requestedJson) {
        if (isBlank(currentJson)) return false;
        try {
            return objectMapper.readTree(currentJson).equals(objectMapper.readTree(requestedJson));
        } catch (JsonProcessingException exception) {
            log.warn("Stored goal survey JSON is invalid; treating the request as a change", exception);
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void queueRoadmapRegeneration(StudentOnboarding onboarding, Long userId) {
        int nextVersion = Optional.ofNullable(onboarding.getRoadmapGenerationVersion()).orElse(0) + 1;
        LocalDateTime now = LocalDateTime.now();
        onboarding.setRoadmapJson(null);
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.PENDING);
        onboarding.setRoadmapGenerationVersion(nextVersion);
        onboarding.setRoadmapGenerationAttempts(0);
        onboarding.setRoadmapLastError(null);
        onboarding.setRoadmapUpdatedAt(now);
        onboardingRepository.save(onboarding);
        roadmapJobService.enqueue(
                userId, nextVersion, onboarding.getPlacementCefrLevel(), onboarding.getGoalSurveyJson());
    }

    private boolean isRoadmapReady(StudentOnboarding onboarding) {
        return onboarding.getRoadmapJson() != null
                && (onboarding.getRoadmapStatus() == null
                || onboarding.getRoadmapStatus() == RoadmapGenerationStatus.READY);
    }

    private RoadmapGenerationStatus effectiveRoadmapStatus(StudentOnboarding onboarding) {
        if (isRoadmapReady(onboarding)) return RoadmapGenerationStatus.READY;
        return onboarding.getRoadmapStatus();
    }

    private boolean isRoadmapInProgress(RoadmapGenerationStatus status) {
        return status == RoadmapGenerationStatus.PENDING
                || status == RoadmapGenerationStatus.PROCESSING;
    }
}
