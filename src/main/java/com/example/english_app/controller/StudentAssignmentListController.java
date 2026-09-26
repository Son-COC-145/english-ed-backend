package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentResponse;
import com.example.english_app.entity.enums.StudentAssignmentFilter;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.classroom.AssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Cross-course to-do list for the student app; per-course list and submit live in StudentAssignmentController. */
@RestController
@RequestMapping("/api/v1/student/assignments")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student Assignment", description = "Student Assignment API")
public class StudentAssignmentListController {

    private final AssignmentService assignmentService;
    private final UserRepository userRepository;

    @Operation(summary = "Bài tập của tôi ở mọi khóa đang học",
            description = "status: ALL | TODO (chưa nộp) | SUBMITTED (chờ chấm, gồm nộp trễ) | GRADED. Sắp xếp theo hạn nộp gần nhất; bài không có hạn ở cuối.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponse>>> getMyAssignments(
            @RequestParam(defaultValue = "ALL") StudentAssignmentFilter status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // Order is fixed by the query (nearest deadline first), so the page request is unsorted.
        return ResponseEntity.ok(ApiResponse.success(
                assignmentService.getStudentAssignments(getCurrentStudent().getId(), status, PageRequest.of(page, size))));
    }

    private User getCurrentStudent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }
}
