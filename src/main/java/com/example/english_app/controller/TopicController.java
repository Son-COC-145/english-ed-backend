package com.example.english_app.controller;

import com.example.english_app.dto.request.TopicRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.TopicResponse;
import com.example.english_app.service.vocabulary.TopicService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Topic", description = "Quản lý topic")
public class TopicController {
    private final TopicService topicService;

    @Operation(summary = "Lấy topic theo id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> getById(@PathVariable("id") Short id) {
        TopicResponse topicResponse = topicService.getById(id);

        return ResponseEntity.ok(ApiResponse.success(topicResponse));
    }

    @Operation(summary = "Lấy danh sách chủ đề (Có phân trang và lọc)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TopicResponse>>> getAll(
            @RequestParam(required = false) String nameSearch,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        PageResponse<TopicResponse> response = topicService.filterTopics(nameSearch, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo chủ đề mới")
    @PostMapping
    public ResponseEntity<ApiResponse<TopicResponse>> create(@Valid @RequestBody TopicRequest request) {
        TopicResponse response = topicService.create(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin chủ đề")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> update(
            @PathVariable("id") Short id,
            @Valid @RequestBody TopicRequest request) {
        TopicResponse response = topicService.update(id, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa chủ đề")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Short id) {
        topicService.delete(id);

        return ResponseEntity.ok(ApiResponse.success("Xóa topic thành công"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt topic")
    @PatchMapping("/activate/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> activate(@PathVariable("id") Short id) {
        TopicResponse response = topicService.activate(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hủy kích hoạt topic")
    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<ApiResponse<TopicResponse>> deactivate(@PathVariable("id") Short id) {
        TopicResponse response = topicService.deactivate(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
