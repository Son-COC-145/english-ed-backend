package com.example.english_app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.english_app.dto.request.classroom.SyllabusItemRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.SyllabusItemResponse;
import com.example.english_app.service.classroom.SyllabusService;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/syllabus")
@RequiredArgsConstructor
@Tag(name = "Syllabus", description = "Syllabus API")
public class SyllabusController {

    private final SyllabusService syllabusService;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách giáo trình của lớp")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SyllabusItemResponse>>> getSyllabus(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Short weekNumber,
            @PageableDefault(sort = "sortOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                syllabusService.getSyllabusByCourseWithFilters(
                        getCurrentUser().getId(), courseId, keyword, weekNumber, pageable)));
    }

    @Operation(summary = "Tạo mới giáo trình")
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SyllabusItemResponse>> createSyllabusItem(
            @PathVariable Long courseId,
            @Valid @RequestBody SyllabusItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                syllabusService.createSyllabusItem(getCurrentUser().getId(), courseId, request)));
    }

    @Operation(summary = "Cập nhật giáo trình")
    @PutMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SyllabusItemResponse>> updateSyllabusItem(
            @PathVariable Long courseId,
            @PathVariable Long itemId,
            @Valid @RequestBody SyllabusItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                syllabusService.updateSyllabusItem(getCurrentUser().getId(), courseId, itemId, request)));
    }

    @Operation(summary = "Xóa giáo trình")
    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSyllabusItem(
            @PathVariable Long courseId,
            @PathVariable Long itemId) {
        syllabusService.deleteSyllabusItem(getCurrentUser().getId(), courseId, itemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
