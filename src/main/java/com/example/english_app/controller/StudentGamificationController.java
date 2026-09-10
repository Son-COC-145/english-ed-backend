package com.example.english_app.controller;

import com.example.english_app.dto.request.ReviewSubmitRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.DailyMissionResponse;
import com.example.english_app.dto.response.DueReviewPageResponse;
import com.example.english_app.dto.response.MinigameResultDetailResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.ReviewSubmitResponse;
import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.StudentVocabularyProgressResponse;
import com.example.english_app.dto.response.VocabularySummaryResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearningStatus;
import com.example.english_app.service.gamification.GameficationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Gamification Progress", description = "Lấy tiến trình học tập, trạng thái từ vựng và lịch sử trò chơi của học viên")
public class StudentGamificationController {

    private final GameficationService gameficationService;

    // ─── Stats & Mission ─────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông số học tập (StudentStat) của người dùng hiện tại")
    @GetMapping("/stat")
    public ResponseEntity<ApiResponse<StudentStatResponse>> getStudentStat() {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getStudentStat()));
    }

    @Operation(summary = "Lấy danh sách nhiệm vụ học hàng ngày (Daily Mission)")
    @GetMapping("/daily-mission")
    public ResponseEntity<ApiResponse<DailyMissionResponse>> getDailyMission() {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getDailyMission()));
    }

    // ─── Vocabulary Summary ──────────────────────────────────────────────────

    @Operation(
        summary = "Lấy tóm tắt tiến trình từ vựng (cho màn Home)",
        description = "Trả về số lượng từ theo từng trạng thái và số từ đến hạn ôn hôm nay. " +
                      "Gọi endpoint này thay vì tải toàn bộ progress để đếm."
    )
    @GetMapping("/vocabulary-summary")
    public ResponseEntity<ApiResponse<VocabularySummaryResponse>> getVocabularySummary() {
        return ResponseEntity.ok(ApiResponse.success(gameficationService.getVocabularySummary()));
    }

    // ─── SRS Review Queue ────────────────────────────────────────────────────

    @Operation(
        summary = "Lấy danh sách từ đến hạn ôn tập theo SRS",
        description = "Backend tính toán queue dựa trên nextReviewAt theo thời gian server (UTC). " +
                      "Mobile không tự suy luận 'đến hạn'. " +
                      "Nếu không có từ nào đến hạn, trả danh sách rỗng và dueCount = 0."
    )
    @GetMapping("/vocabulary-reviews/due")
    public ResponseEntity<ApiResponse<DueReviewPageResponse>> getDueReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Lọc theo chủ đề (optional)")
            @RequestParam(required = false) Short topicId,
            @Parameter(description = "Lọc theo CEFR level (optional)")
            @RequestParam(required = false) CefrLevel cefrLevel) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.getDueReviews(page, size, topicId, cefrLevel)));
    }

    @Operation(
        summary = "Gửi kết quả đánh giá Flashcard/SRS (AGAIN / HARD / FAIR / GOOD / EASY)",
        description = "Backend cập nhật SM-2, tính nextReviewAt và trả kết quả mới. " +
                      "Mobile chỉ gửi rating và hiển thị kết quả. " +
                      "Truyền attemptId để chống duplicate submit khi retry."
    )
    @PostMapping("/vocabulary-reviews/submit")
    public ResponseEntity<ApiResponse<ReviewSubmitResponse>> submitReview(
            @Valid @RequestBody ReviewSubmitRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.processReviewSubmit(request)));
    }

    // ─── Vocabulary Progress ─────────────────────────────────────────────────

    @Operation(
        summary = "Lấy danh sách tiến trình học từ vựng (phân trang, có filter)",
        description = "Hỗ trợ filter theo status (NEW/LEARNING/REVIEWING/MASTERED) và dueOnly. " +
                      "Filter thực hiện server-side; không tải toàn bộ rồi lọc client."
    )
    @GetMapping("/vocabulary-progress")
    public ResponseEntity<ApiResponse<PageResponse<StudentVocabularyProgressResponse>>> getVocabularyProgresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "lastPracticedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Lọc theo trạng thái (optional)")
            @RequestParam(required = false) LearningStatus status,
            @Parameter(description = "Chỉ lấy từ đến hạn ôn (optional)")
            @RequestParam(defaultValue = "false") boolean dueOnly) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.getVocabularyProgresses(status, dueOnly, pageable)));
    }

    @Operation(summary = "Xem chi tiết một tiến trình học từ vựng")
    @GetMapping("/vocabulary-progress/{id}")
    public ResponseEntity<ApiResponse<StudentVocabularyProgressResponse>> getVocabularyProgressDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.getVocabularyProgress(id)));
    }

    // ─── Minigame Results ────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách lịch sử chơi minigame (phân trang)")
    @GetMapping("/minigame-results")
    public ResponseEntity<ApiResponse<PageResponse<MinigameResultDetailResponse>>> getMinigameResults(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "playedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.getMinigameResults(pageable)));
    }

    @Operation(summary = "Xem chi tiết một kết quả chơi minigame")
    @GetMapping("/minigame-results/{id}")
    public ResponseEntity<ApiResponse<MinigameResultDetailResponse>> getMinigameResultDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(gameficationService.getMinigameResult(id)));
    }
}
