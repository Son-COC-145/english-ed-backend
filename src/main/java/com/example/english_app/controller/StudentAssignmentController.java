package com.example.english_app.controller;

import com.example.english_app.dto.request.classroom.AssignmentSubmissionRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.classroom.AssignmentService;
import com.example.english_app.service.classroom.AssignmentSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/student/courses/{courseId}/assignments")
@RequiredArgsConstructor
@Tag(name = "Student Assignment", description = "Student Assignment API")
public class StudentAssignmentController {

    private final AssignmentService assignmentService;
    private final AssignmentSubmissionService submissionService;
    private final UserRepository userRepository;

    private User getCurrentStudent() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách bài tập trong khoá học")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponse>>> getAssignments(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        // Tái sử dụng hàm của AssignmentService
        return ResponseEntity
                .ok(ApiResponse.success(assignmentService.getAssignmentsByCourse(courseId, keyword, pageable)));
    }

    @Operation(summary = "Nộp kết quả bài tập")
    @PostMapping("/{assignmentId}/submit")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> submitAssignment(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request) {
        User student = getCurrentStudent();
        return ResponseEntity
                .ok(ApiResponse.success(submissionService.submitAssignment(student.getId(), assignmentId, request)));
    }
}
