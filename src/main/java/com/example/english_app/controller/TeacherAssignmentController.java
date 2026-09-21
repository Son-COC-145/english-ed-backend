package com.example.english_app.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.english_app.dto.request.classroom.AssignmentRequest;
import com.example.english_app.dto.request.classroom.GradeSubmissionRequest;
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
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teacher/courses/{courseId}/assignments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
@Tag(name = "Teacher Assignment", description = "Teacher Assignment API")
public class TeacherAssignmentController {
    private final AssignmentService assignmentService;
    private final AssignmentSubmissionService submissionService;
    private final UserRepository userRepository;

    private User getCurrentTeacher() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách bài tập của khóa học")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AssignmentResponse>>> getAssigmnet(
            @PathVariable Long courseId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(assignmentService.getAssignmentsForTeacher(getCurrentTeacher().getId(), courseId, keyword, pageable)));
    }

    @Operation(summary = "Tạo mới bài tập")
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> createAssignment(
            @PathVariable Long courseId,
            @Valid @RequestBody AssignmentRequest request) {
        User teacher = getCurrentTeacher();
        return ResponseEntity
                .ok(ApiResponse.success(assignmentService.createAssignment(teacher.getId(), courseId, request)));
    }

    @Operation(summary = "Cập nhật bài tập")
    @PutMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> updateAssignment(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(assignmentService.updateAssignment(
                getCurrentTeacher().getId(), courseId, assignmentId, request)));
    }

    @Operation(summary = "Xóa bài tập")
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAssignment(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId) {
        assignmentService.deleteAssignment(getCurrentTeacher().getId(), courseId, assignmentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Lấy danh sách bài nộp của 1 bài tập")
    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<ApiResponse<PageResponse<AssignmentSubmissionResponse>>> getSubmissions(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @PageableDefault(sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity
                .ok(ApiResponse.success(submissionService.getSubmissionsByAssignment(
                        getCurrentTeacher().getId(), courseId, assignmentId, pageable)));
    }

    @Operation(summary = "Chấm điểm bài nộp")
    @PutMapping("/{assignmentId}/submissions/{submissionId}/grade")
    public ResponseEntity<ApiResponse<AssignmentSubmissionResponse>> gradeSubmission(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeSubmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(submissionService.gradeSubmission(
                getCurrentTeacher().getId(), courseId, assignmentId, submissionId, request)));
    }
}
