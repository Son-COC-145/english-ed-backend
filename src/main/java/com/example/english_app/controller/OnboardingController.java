package com.example.english_app.controller;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.RoadmapGenerationStatusResponse;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.service.onboarding.OnboardingLifecycleService;
import com.example.english_app.service.onboarding.PlacementTestService;
import com.example.english_app.service.onboarding.PronunciationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Onboarding", description = "Module 0: Onboarding & Placement Test")
public class OnboardingController {

    private final OnboardingLifecycleService lifecycleService;
    private final PlacementTestService placementTestService;
    private final PronunciationService pronunciationService;

    // Onboarding Status
    @Operation(summary = "Lấy trạng thái onboarding hiện tại")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<OnboardingStatusResponse>> getStatus(Authentication auth) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.success(lifecycleService.getStatus(userId(auth))));
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

            Thiết kế đúng: 1 bài 20 câu chỉ cần **21 requests** (1 start + 20 submit),
            vì submit câu thứ 20 đã trả luôn placementResult,
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

    @Operation(summary = "Nộp câu trả lời phát âm (audio) — trả kèm điểm số và câu tiếp theo",
            description = """
                    Nhận multipart/form-data gồm các **form fields** (không phải query params):
                    - `sessionId` (Long, bắt buộc): ID phiên làm bài
                    - `questionId` (Long, bắt buộc): ID câu hỏi hiện tại
                    - `submissionId` (UUID, bắt buộc): Flutter giữ nguyên UUID cho mọi retry của cùng bản ghi âm
                    - `audioFile` (MultipartFile, bắt buộc): File ghi âm WAV/WebM/OGG
                    """)
    @PostMapping(value = "/placement-test/pronunciation/submit-answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.example.english_app.dto.response.PlacementPronunciationAnswerResponse>> submitPronunciationAnswer(
            Authentication auth,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("questionId") Long questionId,
            @RequestParam("submissionId") UUID submissionId,
            @RequestPart("audioFile") MultipartFile audioFile) {

        // Validate: audioFile phải có dữ liệu
        if (audioFile == null || audioFile.isEmpty()) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.AUDIO_EMPTY_OR_CORRUPT);
        }

        log.debug("[PronunciationSubmit] userId={}, sessionId={}, questionId={}, submissionId={}, audioSize={}B",
                userId(auth), sessionId, questionId, submissionId, audioFile.getSize());

        com.example.english_app.dto.response.PlacementPronunciationAnswerResponse result =
                pronunciationService.submitPronunciationWithProgression(
                        userId(auth), sessionId, questionId, submissionId, audioFile);
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

    @Operation(summary = "Lấy trạng thái tạo lộ trình để client polling")
    @GetMapping("/roadmap/status")
    public ResponseEntity<ApiResponse<RoadmapGenerationStatusResponse>> getRoadmapStatus(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                lifecycleService.getRoadmapGenerationStatus(userId(auth))));
    }

    @Operation(summary = "Yêu cầu tạo lại lộ trình sau khi worker thất bại")
    @PostMapping("/roadmap/retry")
    public ResponseEntity<ApiResponse<RoadmapGenerationStatusResponse>> retryRoadmap(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                lifecycleService.retryRoadmap(userId(auth))));
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

    // Helper
    private Long userId(Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        Number id = jwt.getClaim("userId");
        if (id == null) {
            throw new com.example.english_app.exception.AppException(
                    com.example.english_app.exception.ErrorCode.UNAUTHORIZED);
        }
        return id.longValue();
    }
}
