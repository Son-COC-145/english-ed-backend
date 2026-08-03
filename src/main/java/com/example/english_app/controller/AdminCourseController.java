package com.example.english_app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.english_app.dto.request.classroom.CourseRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.CourseResponse;
import com.example.english_app.service.classroom.CourseService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/courses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseController {

    private final CourseService courseService;

    @Operation(summary = "Lấy danh sách khóa học")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getAllCourses(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(courseService.getAllCourses(pageable)));
    }

    @Operation(summary = "Lấy danh sách khóa học theo giáo viên")
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getCoursesByTeacher(
            @PathVariable Long teacherId,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(courseService.getCoursesByTeacher(teacherId, pageable)));
    }

    @Operation(summary = "Tạo mới khóa học")
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.createCourse(request)));
    }

    @Operation(summary = "Cập nhật khóa học")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseService.updateCourse(id, request)));
    }

    @Operation(summary = "Kích hoạt khóa học")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<CourseResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.activate(id)));
    }

    @Operation(summary = "Hủy kích hoạt khóa học")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<CourseResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(courseService.deactivate(id)));
    }

    @Operation(summary = "Xóa khóa học")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
