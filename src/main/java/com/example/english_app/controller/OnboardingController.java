package com.example.english_app.controller;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.service.onboarding.OnboardingService;
import com.example.english_app.service.onboarding.PronunciationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Onboarding", description = "Module 0: Onboarding & Placement Test")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final PronunciationService pronunciationService;

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

    @Operation(summary = "Nộp câu trả lời phát âm (audio)")
    @PostMapping(value = "/placement-test/pronunciation/submit-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PronunciationScoreResult>> submitPronunciationAnswer(
            Authentication auth,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("questionId") Long questionId,
            @RequestPart("audioFile") MultipartFile audioFile,
            @RequestParam("word") String word,
            @RequestParam("wordIndex") int wordIndex) {

        Long userId = getUserId(auth);
        PronunciationScoreResult result = pronunciationService.submitPronunciation(
                userId, sessionId, questionId, audioFile, word, wordIndex);
        
        return ResponseEntity.ok(ApiResponse.success(result));
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

    @Operation(summary = "Lấy lộ trình học tập")
    @GetMapping("/roadmap")
    public ResponseEntity<ApiResponse<RoadmapResponse>> getRoadmap(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(ApiResponse.success(
                onboardingService.getRoadmap(userId)));
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

    @Operation(summary = "Reset Onboarding (Dành cho test/Dev)")
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetOnboarding(Authentication auth) {
        Long userId = getUserId(auth);
        onboardingService.resetOnboarding(userId);
        return ResponseEntity.ok(ApiResponse.success("Reset thành công. Hãy tải lại trang để làm lại."));
    }

    //  HELPER

    private Long getUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Number userId = jwt.getClaim("userId");
        return userId != null ? userId.longValue() : null;
    }
}
