package com.example.english_app.controller;

import com.example.english_app.dto.request.SpeakingScenarioRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.SpeakingScenarioResponse;
import com.example.english_app.service.speaking.SpeakingScenarioService;
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

@RestController
@RequestMapping("/api/v1/admin/speaking-scenarios")
@RequiredArgsConstructor
@Tag(name = "Admin Speaking Scenarios", description = "API quản lý kịch bản giao tiếp dành cho Admin hoặc Giáo viên")
public class AdminSpeakingScenarioController {

    private final SpeakingScenarioService speakingScenarioService;

    @Operation(summary = "Lấy danh sách kịch bản giao tiếp (có lọc và phân trang)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SpeakingScenarioResponse>>> getScenarios(
            @RequestParam(required = false) Short id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Short topicId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(speakingScenarioService.filterScenarios(id, title, topicId, isActive, pageable)));
    }

    @Operation(summary = "Lấy thông tin chi tiết một kịch bản giao tiếp theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SpeakingScenarioResponse>> getScenarioById(@PathVariable Short id) {
        return ResponseEntity.ok(ApiResponse.success(speakingScenarioService.getScenarioById(id)));
    }

    @Operation(summary = "Tạo mới một kịch bản giao tiếp")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SpeakingScenarioResponse>> createScenario(
            @Valid @RequestBody SpeakingScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(speakingScenarioService.create(request), "Tạo kịch bản thành công"));
    }

    @Operation(summary = "Cập nhật thông tin kịch bản giao tiếp")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<SpeakingScenarioResponse>> updateScenario(@PathVariable Short id,
            @Valid @RequestBody SpeakingScenarioRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success(speakingScenarioService.update(id, request), "Cập nhật kịch bản thành công"));
    }

    @Operation(summary = "Xóa một kịch bản giao tiếp")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(@PathVariable Short id) {
        speakingScenarioService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa kịch bản thành công"));
    }

    @Operation(summary = "Kích hoạt (Mở) một kịch bản giao tiếp")
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> activateScenario(@PathVariable Short id) {
        speakingScenarioService.activate(id);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt kịch bản thành công"));
    }

    @Operation(summary = "Vô hiệu hóa (Tắt) một kịch bản giao tiếp")
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Void>> deactivateScenario(@PathVariable Short id) {
        speakingScenarioService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa kịch bản thành công"));
    }
}
