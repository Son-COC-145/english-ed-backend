package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.example.english_app.dto.request.classroom.CourseStudentRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.CourseResponse;
import com.example.english_app.dto.response.classroom.CourseStudentDetailResponse;
import com.example.english_app.dto.response.classroom.CourseStudentResponse;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.classroom.CourseService;
import com.example.english_app.service.classroom.CourseStudentService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teacher/courses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
public class TeacherCourseController {

    private final CourseService courseService;
    private final CourseStudentService courseStudentService;
    private final UserRepository userRepository;

    private User getCurrentTeacher() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách khóa học của giáo viên")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getMyCourses(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        User teacher = getCurrentTeacher();
        return ResponseEntity.ok(
                ApiResponse.success(courseService.getCoursesByTeacher(teacher.getId(), pageable)));
    }

    @Operation(summary = "Lấy danh sách học viên trong khóa học")
    @GetMapping("/{courseId}/students")
    public ResponseEntity<ApiResponse<PageResponse<CourseStudentDetailResponse>>> getStudentsInCourse(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ClassStudentStatus status,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                        ApiResponse.success(
                        courseStudentService.getStudentsByCourseWithStats(getCurrentTeacher().getId(), courseId, keyword, status, pageable)));
    }

    @Operation(summary = "Thêm học viên vào khóa học")
    @PostMapping("/{courseId}/students")
    public ResponseEntity<ApiResponse<CourseStudentResponse>> addStudentToCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseStudentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(courseStudentService.addStudentToCourse(
                getCurrentTeacher().getId(), courseId, request)));
    }

    @Operation(summary = "Xóa học viên khỏi khóa học")
    @DeleteMapping("/{courseId}/students/{studentId}")
    public ResponseEntity<ApiResponse<Void>> removeStudentFromCourse(
            @PathVariable Long courseId,
            @PathVariable Long studentId) {
        courseStudentService.removeStudentFromCourse(getCurrentTeacher().getId(), courseId, studentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
