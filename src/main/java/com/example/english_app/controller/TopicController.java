package com.example.english_app.controller;

import com.example.english_app.dto.request.TopicRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.TopicResponse;
import com.example.english_app.service.vocabulary.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/topic")
@RequiredArgsConstructor
@Tag(name = "Topic", description = "Quản lý chủ đề từ vựng (Topic)")
public class TopicController {
    private final TopicService topicService;

    @Operation(
        summary = "Lấy chi tiết chủ đề theo ID",
        description = "Trả về thông tin chi tiết chủ đề cùng số lượng từ vựng (vocabularyCount) và số từ học viên đã thuộc (masteredCount nếu đã đăng nhập)."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> getById(
            @Parameter(description = "ID của chủ đề", example = "1")
            @PathVariable("id") Short id) {
        TopicResponse topicResponse = topicService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(topicResponse));
    }

    @Operation(
        summary = "Lấy danh sách chủ đề (Có phân trang và tìm kiếm)",
        description = "Hỗ trợ tìm kiếm theo tên (Anh/Việt) và lọc theo trạng thái kích hoạt. Trả kèm vocabularyCount và masteredCount cho từng chủ đề."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TopicResponse>>> getAll(
            @Parameter(description = "Từ khóa tìm kiếm theo tên chủ đề (Anh hoặc Việt)")
            @RequestParam(required = false) String nameSearch,
            @Parameter(description = "Lọc theo trạng thái kích hoạt (true/false)")
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        PageResponse<TopicResponse> response = topicService.filterTopics(nameSearch, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo chủ đề mới (Chỉ dành cho ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<TopicResponse>> create(@Valid @RequestBody TopicRequest request) {
        TopicResponse response = topicService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin chủ đề (Chỉ dành cho ADMIN)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> update(
            @Parameter(description = "ID của chủ đề cần sửa", example = "1")
            @PathVariable("id") Short id,
            @Valid @RequestBody TopicRequest request) {
        TopicResponse response = topicService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa chủ đề (Chỉ dành cho ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "ID của chủ đề cần xóa", example = "1")
            @PathVariable("id") Short id) {
        topicService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa topic thành công"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt chủ đề (Chỉ dành cho ADMIN)")
    @PatchMapping("/activate/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> activate(
            @Parameter(description = "ID của chủ đề", example = "1")
            @PathVariable("id") Short id) {
        TopicResponse response = topicService.activate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hủy kích hoạt chủ đề (Chỉ dành cho ADMIN)")
    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> deactivate(
            @Parameter(description = "ID của chủ đề", example = "1")
            @PathVariable("id") Short id) {
        TopicResponse response = topicService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
