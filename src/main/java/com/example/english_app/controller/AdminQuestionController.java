package com.example.english_app.controller;

import com.example.english_app.dto.request.AdminQuestionRequest;
import com.example.english_app.dto.response.AdminQuestionResponse;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.QuestionBankStatsResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.service.onboarding.AdminQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Placement Questions", description = "Quản lý ngân hàng câu hỏi kiểm tra phân loại đầu vào (Placement Test)")
public class AdminQuestionController {

    private final AdminQuestionService adminQuestionService;

    // ─── List / Detail ─────────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách câu hỏi (lọc theo CEFR level, Kỹ năng, Trạng thái, phân trang)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminQuestionResponse>>> getQuestions(
            @RequestParam(required = false) CefrLevel level,
            @RequestParam(required = false) Skill skill,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                adminQuestionService.getQuestions(level, skill, isActive, pageable)));
    }

    @Operation(summary = "Lấy chi tiết một câu hỏi theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminQuestionResponse>> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminQuestionService.getQuestionById(id)));
    }

    // ─── Stats ─────────────────────────────────────────────────────────────────

    @Operation(summary = "Thống kê ngân hàng câu hỏi: tổng active/inactive, breakdown theo CEFR level và Kỹ năng")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<QuestionBankStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminQuestionService.getStats()));
    }

    // ─── Create / Update / Delete ──────────────────────────────────────────────

    @Operation(summary = "Thêm câu hỏi mới vào ngân hàng đề thi")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminQuestionResponse>> createQuestion(
            @Valid @RequestBody AdminQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(adminQuestionService.createQuestion(request), "Tạo câu hỏi thành công"));
    }

    @Operation(summary = "Cập nhật toàn bộ nội dung câu hỏi, đáp án, điểm độ khó")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminQuestionResponse>> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody AdminQuestionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminQuestionService.updateQuestion(id, request), "Cập nhật câu hỏi thành công"));
    }

    @Operation(summary = "Xóa câu hỏi khỏi ngân hàng đề thi")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteQuestion(@PathVariable Long id) {
        adminQuestionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Xóa câu hỏi thành công")));
    }

    // ─── Toggle Active / Deactivate ────────────────────────────────────────────

    @Operation(summary = "Kích hoạt câu hỏi (đưa vào vòng quay bài test)")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminQuestionResponse>> activateQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminQuestionService.toggleActive(id, true), "Đã kích hoạt câu hỏi"));
    }

    @Operation(summary = "Vô hiệu hoá câu hỏi (rút khỏi vòng quay, không xóa data)")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminQuestionResponse>> deactivateQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                adminQuestionService.toggleActive(id, false), "Đã vô hiệu hoá câu hỏi"));
    }
}
