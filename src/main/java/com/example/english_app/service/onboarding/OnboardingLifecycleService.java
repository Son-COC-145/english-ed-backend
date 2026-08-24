package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.gamification.DailyGoal;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.DailyGoalRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.user.UserRepository;
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
     * <p><b>PERF:</b> Dùng {@code findByStudentIdWithUser} (JOIN FETCH) để load
     * {@code StudentOnboarding} và {@code User} trong 1 query thay vì 2 query riêng.
     * Endpoint này được gọi khi app khởi động/login nên cần cực kỳ nhẹ.
     */
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getStatus(Long userId) {
        // Happy path (phổ biến): user đã bắt đầu onboarding → 1 query lấy cả 2
        Optional<StudentOnboarding> opt = onboardingRepository.findByStudentIdWithUser(userId);

        if (opt.isEmpty()) {
            // Lần đầu dùng app – chưa có StudentOnboarding record
            // Cần query User riêng để lấy fullName
            User user = findUser(userId);
            return OnboardingStatusResponse.builder()
                    .goalSurveyCompleted(false)
                    .placementTestCompleted(false)
                    .settingsCompleted(false)
                    .onboardingCompleted(false)
                    .nextStep("GOAL_SURVEY")
                    .stepNumber(STEP_GOAL_SURVEY)
                    .totalSteps(TOTAL_STEPS)
                    .userName(user.getFullName())
                    .roadmapGenerated(false)
                    .build();
        }

        StudentOnboarding ob = opt.get();
        // ob.getStudent() đã được JOIN FETCH → không tốn thêm query
        String userName     = ob.getStudent().getFullName();
        boolean goalDone      = ob.getGoalSurveyJson() != null;
        boolean placementDone = ob.getPlacementCefrLevel() != null;
        boolean roadmapDone   = ob.getRoadmapJson() != null;
        boolean settingsDone  = ob.getDailyGoalXp() != null && ob.getDailyGoalXp() > 0;

        // Xác định bước tiếp theo theo thứ tự tuyến tính
        String nextStep;
        int stepNumber;
        if (!goalDone) {
            nextStep   = "GOAL_SURVEY";    stepNumber = STEP_GOAL_SURVEY;
        } else if (!placementDone) {
            nextStep   = "PLACEMENT_TEST"; stepNumber = STEP_PLACEMENT_TEST;
        } else if (!roadmapDone) {
            // Roadmap đang được sinh hoặc chưa sẵn — giữ user ở bước xem roadmap
            nextStep   = "ROADMAP_VIEW";   stepNumber = STEP_ROADMAP_VIEW;
        } else if (!settingsDone || !ob.getOnboardingCompleted()) {
            nextStep   = "SETTINGS";       stepNumber = STEP_SETTINGS;
        } else {
            nextStep   = "COMPLETED";      stepNumber = STEP_COMPLETED;
        }

        return OnboardingStatusResponse.builder()
                .goalSurveyCompleted(goalDone)
                .placementTestCompleted(placementDone)
                .settingsCompleted(settingsDone)
                .onboardingCompleted(ob.getOnboardingCompleted())
                .nextStep(nextStep)
                .stepNumber(stepNumber)
                .totalSteps(TOTAL_STEPS)
                .userName(userName)
                .placementCefrLevel(placementDone ? ob.getPlacementCefrLevel().name() : null)
                .dailyGoalXp(ob.getDailyGoalXp())
                .roadmapGenerated(roadmapDone)
                .build();
    }

    // ─── Goal Survey ──────────────────────────────────────────────────────────

    /** Lưu kết quả khảo sát mục tiêu. Tạo StudentOnboarding record nếu chưa có. */
    public void submitGoalSurvey(Long userId, GoalSurveyRequest request) {
        User user = findUser(userId);

        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseGet(() -> StudentOnboarding.builder().student(user).build());

        try {
            ob.setGoalSurveyJson(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize goal survey for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }

        onboardingRepository.save(ob);
    }

    // ─── Roadmap ──────────────────────────────────────────────────────────────

    /**
     * Đọc lộ trình đã được sinh ra và lưu trong StudentOnboarding.
     * Lộ trình được tạo bởi RoadmapGenerationService (trong PlacementTestService).
     * Đây chỉ là đọc — không sinh lại.
     */
    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(Long userId) {
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        if (ob.getRoadmapJson() == null) {
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }

        try {
            return objectMapper.readValue(ob.getRoadmapJson(), RoadmapResponse.class);
        } catch (Exception e) {
            log.error("Failed to parse roadmap_json for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    /**
     * Lấy tiến độ học tập trên lộ trình (Roadmap Progress).
     */
    @Transactional(readOnly = true)
    public RoadmapProgressResponse getRoadmapProgress(Long userId) {
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        if (ob.getRoadmapJson() == null) {
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }

        try {
            com.example.english_app.dto.response.roadmap.RoadmapResponse roadmap = 
                    objectMapper.readValue(ob.getRoadmapJson(), com.example.english_app.dto.response.roadmap.RoadmapResponse.class);

            List<RoadmapMilestone> milestones = roadmap.getMilestones() != null ? roadmap.getMilestones() : Collections.emptyList();
            int totalWeeks = milestones.size();
            int totalModules = 0;
            String nextModule = null;

            for (RoadmapMilestone milestone : milestones) {
                if (milestone.getModules() != null) {
                    totalModules += milestone.getModules().size();
                    if (nextModule == null && !milestone.getModules().isEmpty()) {
                        nextModule = milestone.getModules().get(0).getTitle();
                    }
                }
            }

            int completedModules = 0;
            int completedWeeks = 0;
            int currentWeek = totalWeeks > 0 ? 1 : 0;
            double percent = totalModules > 0 ? ((double) completedModules / totalModules) * 100.0 : 0.0;

            return RoadmapProgressResponse.builder()
                    .cefrLevel(roadmap.getCefrLevel())
                    .totalWeeks(totalWeeks)
                    .completedWeeks(completedWeeks)
                    .currentWeek(currentWeek)
                    .totalModules(totalModules)
                    .completedModules(completedModules)
                    .percentCompleted(Math.round(percent * 10.0) / 10.0)
                    .nextSuggestedModule(nextModule)
                    .milestones(milestones)
                    .build();

        } catch (Exception e) {
            log.error("Failed to calculate roadmap progress for user {}", userId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    // ─── Settings ─────────────────────────────────────────────────────────────

    /** Lưu cài đặt cá nhân hoá và khởi tạo DailyGoal + StudentStat nếu chưa có. */
    public void saveSettings(Long userId, OnboardingSettingsRequest request) {
        User user = findUser(userId);

        if (!request.isValidDailyGoalXp()) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

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
        StudentOnboarding ob = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.SYSTEM_ERROR.toException());

        if (ob.getOnboardingCompleted()) {
            throw ErrorCode.ONBOARDING_ALREADY_COMPLETED.toException();
        }

        boolean allDone = ob.getGoalSurveyJson() != null
                && ob.getPlacementCefrLevel() != null
                && ob.getDailyGoalXp() != null && ob.getDailyGoalXp() > 0;

        if (!allDone) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        ob.setOnboardingCompleted(true);
        ob.setOnboardingCompletedAt(LocalDateTime.now());
        onboardingRepository.save(ob);
        log.info("Onboarding completed for user {}", userId);
    }

    /**
     * Reset toàn bộ kết quả placement test (chỉ ADMIN được gọi — guard ở Controller).
     */
    public void resetOnboarding(Long userId) {
        onboardingRepository.findByStudentId(userId).ifPresent(ob -> {
            ob.setPlacementCefrLevel(null);
            ob.setPlacementVocabScore(null);
            ob.setPlacementGrammarScore(null);
            ob.setPlacementReadingScore(null);
            ob.setPlacementListeningScore(null);
            ob.setPlacementPronunciationScore(null);
            ob.setPlacementCompletedAt(null);
            ob.setOnboardingCompleted(false);
            ob.setOnboardingCompletedAt(null);
            ob.setRoadmapJson(null);
            onboardingRepository.save(ob);
            log.info("Onboarding reset for user {}", userId);
        });

        sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(userId)
                .ifPresent(s -> {
                    s.setIsCompleted(true);
                    sessionRepository.save(s);
                });
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }
}
