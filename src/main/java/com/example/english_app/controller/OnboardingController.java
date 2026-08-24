package com.example.english_app.controller;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.service.onboarding.OnboardingLifecycleService;
import com.example.english_app.service.onboarding.PlacementTestService;
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

    private final OnboardingLifecycleService lifecycleService;
    private final PlacementTestService placementTestService;
    private final PronunciationService pronunciationService;

    // Onboarding Status
    @Operation(summary = "Lấy trạng thái onboarding hiện tại")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                lifecycleService.getStatus(userId(auth))));
    }

    // Goal Survey
    @Operation(summary = "Nộp khảo sát mục tiêu học tập")
    @PostMapping("/goal-survey")
    public ResponseEntity<ApiResponse<Void>> submitGoalSurvey(
            Authentication auth,
            @Valid @RequestBody GoalSurveyRequest request) {
        lifecycleService.submitGoalSurvey(userId(auth), request);
        return ResponseEntity.ok(ApiResponse.success("Lưu khảo sát thành công"));
    }

    // Placement Test
    @Operation(summary = "Bắt đầu bài kiểm tra phân loại trình độ")
    @PostMapping("/placement-test/start")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> startTest(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.startTest(userId(auth))));
    }

    @Deprecated(since = "1.1", forRemoval = false)
    @Operation(summary = "[DEPRECATED] Lấy câu hỏi tiếp theo", description = """
            **Không dùng trong tích hợp mới.**

            Endpoint này sẽ tạo thêm 1 round-trip HTTP không cần thiết.
            - Dùng `POST /placement-test/start` → đã trả luôn `firstQuestion`
            - Dùng `POST /placement-test/submit-answer` → đã trả luôn `nextQuestion` trong cùng response

            Thiết kế đúng: 1 bài 20 câu chỉ cần **22 requests** (1 start + 20 submit + 1 result),
            không phải 40+ requests.
            """, deprecated = true)
    @GetMapping("/placement-test/next-question")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> getNextQuestion(
            Authentication auth,
            @RequestParam Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.getNextQuestion(sessionId, userId(auth))));
    }

    @Operation(summary = "Nộp câu trả lời")
    @PostMapping("/placement-test/submit-answer")
    public ResponseEntity<ApiResponse<PlacementQuestionResponse>> submitAnswer(
            Authentication auth,
            @Valid @RequestBody PlacementAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.submitAnswer(userId(auth), request)));
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

        PronunciationScoreResult result = pronunciationService.submitPronunciation(
                userId(auth), sessionId, questionId, audioFile, word, wordIndex);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Kết thúc bài kiểm tra")
    @PostMapping("/placement-test/complete")
    public ResponseEntity<ApiResponse<PlacementResultResponse>> completeTest(
            Authentication auth,
            @RequestParam Long sessionId) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.completeTest(sessionId, userId(auth))));
    }

    @Operation(summary = "Bỏ qua bài kiểm tra (Dành cho người mới bắt đầu từ con số 0)")
    @PostMapping("/placement-test/skip")
    public ResponseEntity<ApiResponse<PlacementResultResponse>> skipTest(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.skipTest(userId(auth))));
    }

    @Operation(summary = "Xem kết quả bài kiểm tra")
    @GetMapping("/placement-test/result")
    public ResponseEntity<ApiResponse<PlacementResultResponse>> getResult(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                placementTestService.getResult(userId(auth))));
    }

    // Roadmap
    @Operation(summary = "Lấy lộ trình học tập")
    @GetMapping("/roadmap")
    public ResponseEntity<ApiResponse<RoadmapResponse>> getRoadmap(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                lifecycleService.getRoadmap(userId(auth))));
    }

    @Operation(summary = "Lấy tiến độ hoàn thành lộ trình học tập")
    @GetMapping("/roadmap/progress")
    public ResponseEntity<ApiResponse<RoadmapProgressResponse>> getRoadmapProgress(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                lifecycleService.getRoadmapProgress(userId(auth))));
    }

    // Settings
    @Operation(summary = "Lưu cài đặt cá nhân hóa")
    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<Void>> saveSettings(
            Authentication auth,
            @Valid @RequestBody OnboardingSettingsRequest request) {
        lifecycleService.saveSettings(userId(auth), request);
        return ResponseEntity.ok(ApiResponse.success("Lưu cài đặt thành công"));
    }

    @Operation(summary = "Hoàn thành toàn bộ quá trình onboarding")
    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<Void>> completeOnboarding(Authentication auth) {
        lifecycleService.completeOnboarding(userId(auth));
        return ResponseEntity.ok(ApiResponse.success("Chúc mừng bạn đã hoàn thành onboarding!"));
    }

    @Operation(summary = "Reset Onboarding (Chỉ dành cho ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> resetOnboarding(
            @RequestParam Long targetUserId) {
        lifecycleService.resetOnboarding(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Reset onboarding cho userId=" + targetUserId + " thành công."));
    }

    // Helper
    private Long userId(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        Number id = jwt.getClaim("userId");
        return id != null ? id.longValue() : null;
    }
}
