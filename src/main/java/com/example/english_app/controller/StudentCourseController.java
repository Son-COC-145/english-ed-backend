package com.example.english_app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.CourseResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.classroom.CourseStudentService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/student/courses")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseStudentService courseStudentService;
    private final UserRepository userRepository;

    private User getCurrentStudent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách khóa học tôi đang tham gia")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getMyCourses(
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        User student = getCurrentStudent();
        return ResponseEntity.ok(
                ApiResponse.success(courseStudentService.getCoursesByStudent(student.getId(), pageable)));
    }
}
