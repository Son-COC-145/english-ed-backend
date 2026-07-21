package com.example.english_app.controller;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.service.onboarding.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Onboarding", description = "Module 0: Onboarding & Placement Test")
public class OnboardingController {

    private final OnboardingService onboardingService;

    //  ONBOARDING STATUS

    @Operation(summary = "Lấy trạng thái onboarding hiện tại")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getOnboardingStatus(userId)));
    }

    //  GOAL SURVEY

    @Operation(summary = "Nộp khảo sát mục tiêu học tập")
    @PostMapping("/goal-survey")
    public ResponseEntity<ApiResponse<Void>> submitGoalSurvey(
            Authentication auth,
            @Valid @RequestBody GoalSurveyRequest request) {
        Long userId = getUserId(auth);
        onboardingService.submitGoalSurvey(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu khảo sát thành công"));
    }

    //  PLACEMENT TEST

    @Operation(summary = "Bắt đầu bài kiểm tra phân loại trình độ")
    @PostMapping("/placement-test/start")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> startTest(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.startPlacementTest(userId)));
    }

    @Operation(summary = "Lấy câu hỏi tiếp theo")
    @GetMapping("/placement-test/next-question")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> getNextQuestion(
            Authentication auth,
            @RequestParam Long sessionId) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getNextQuestion(sessionId, userId)));
    }

    @Operation(summary = "Nộp câu trả lời")
    @PostMapping("/placement-test/submit-answer")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> submitAnswer(
            Authentication auth,
            @Valid @RequestBody PlacementAnswerRequest request) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.submitAnswer(userId, request)));
    }

    @Operation(summary = "Kết thúc bài kiểm tra")
    @PostMapping("/placement-test/complete")
    public ResponseEntity<ApiResponse<PlacementResultResponse>> completeTest(
            Authentication auth,
            @RequestParam Long sessionId) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.completePlacementTest(sessionId, userId)));
    }

    @Operation(summary = "Xem kết quả bài kiểm tra")
    @GetMapping("/placement-test/result")
    public ResponseEntity<ApiResponse<PlacementResultResponse>> getResult(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getPlacementResult(userId)));
    }

    //  SETTINGS

    @Operation(summary = "Lưu cài đặt cá nhân hóa")
    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> saveSettings(
            Authentication auth,
            @Valid @RequestBody OnboardingSettingsRequest request) {
        Long userId = getUserId(auth);
        onboardingService.saveSettings(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu cài đặt thành công"));
    }

    @Operation(summary = "Hoàn thành toàn bộ quá trình onboarding")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(Authentication auth) {
        Long userId = getUserId(auth);
        onboardingService.completeOnboarding(userId);
        return ResponseEntity.ok(ApiResponse.success("Chúc mừng bạn đã hoàn thành onboarding!"));
    }

    //  HELPER

    private Long getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number userId = jwt.getClaim("userId");
        return userId != null ? userId.longValue() : null;
    }
}
