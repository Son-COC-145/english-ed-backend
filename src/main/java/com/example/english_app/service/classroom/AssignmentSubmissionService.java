package com.example.english_app.service.classroom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.classroom.AssignmentSubmissionRequest;
import com.example.english_app.dto.request.classroom.GradeSubmissionRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.AssignmentSubmission;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentSubmissionService {
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final ClassroomMapper classroomMapper;
    private final NotificationService notificationService;
    private final CourseAccessService courseAccessService;

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(Long studentId, Long courseId, Long assignmentId,
            AssignmentSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        courseAccessService.requireActiveStudent(student.getId(), assignment.getCourse());

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        if (submission.getId() != null && submission.getStatus() == AssignmentSubmissionStatus.GRADED) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        submission.setResultRefId(
                request.getResultRefId() != null ? request.getResultRefId() : submission.getResultRefId());
        LocalDateTime submittedAt = LocalDateTime.now();
        submission.setStatus(assignment.getDeadlineAt() != null && submittedAt.isAfter(assignment.getDeadlineAt())
                ? AssignmentSubmissionStatus.LATE
                : AssignmentSubmissionStatus.SUBMITTED);
        submission.setSubmittedAt(submittedAt);

        return classroomMapper.toAssignmentSubmissionResponse(submissionRepository.save(submission));
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(Long teacherId, Long courseId, Long assignmentId,
            Long submissionId, GradeSubmissionRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> ErrorCode.SUBMISSION_NOT_FOUND.toException());
        if (!submission.getAssignment().getId().equals(assignmentId)
                || !submission.getAssignment().getCourse().getId().equals(courseId)) {
            throw ErrorCode.SUBMISSION_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, submission.getAssignment().getCourse());
        if (request.getStatus() != AssignmentSubmissionStatus.GRADED) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        submission.setScore(request.getScore());
        submission.setTeacherCommentText(request.getTeacherCommentText());
        submission.setTeacherAudioCommentUrl(request.getTeacherAudioCommentUrl());
        submission.setStatus(request.getStatus());
        submission.setCommentedAt(LocalDateTime.now());

        AssignmentSubmission savedSubmission = submissionRepository.save(submission);

        notificationService.sendToUser(savedSubmission.getStudent().getId(), 
            "Đã có điểm", 
            "Bài tập " + savedSubmission.getAssignment().getTitle() + " đã được chấm điểm.", 
            "GRADE");

        return classroomMapper.toAssignmentSubmissionResponse(savedSubmission);
    }

    public PageResponse<AssignmentSubmissionResponse> getSubmissionsByAssignment(Long teacherId, Long courseId,
            Long assignmentId, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, assignment.getCourse());
        Page<AssignmentSubmission> pageResult = submissionRepository.findAllByAssignmentId(assignmentId, pageable);
        List<AssignmentSubmissionResponse> content = pageResult.getContent().stream()
                .map(classroomMapper::toAssignmentSubmissionResponse)
                .collect(Collectors.toList());
        return PageResponse.<AssignmentSubmissionResponse>builder()
                .content(content)
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    public PageResponse<AssignmentSubmissionResponse> getSubmissionsByStudent(Long studentId, Pageable pageable) {
        Page<AssignmentSubmission> pageResult = submissionRepository.findAllByStudentId(studentId, pageable);
        List<AssignmentSubmissionResponse> content = pageResult.getContent().stream()
                .map(classroomMapper::toAssignmentSubmissionResponse)
                .collect(Collectors.toList());
        return PageResponse.<AssignmentSubmissionResponse>builder()
                .content(content)
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }
}
