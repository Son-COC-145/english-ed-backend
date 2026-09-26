package com.example.english_app.service.classroom;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.classroom.AssignmentSubmissionRequest;
import com.example.english_app.dto.request.classroom.GradeSubmissionRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionDetailResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.AssignmentSubmission;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.notification.NotificationOutboxService;
import com.example.english_app.entity.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentSubmissionService {
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper classroomMapper;
    private final NotificationOutboxService notificationOutboxService;
    private final CourseAccessService courseAccessService;
    private final AssignmentReferenceService referenceService;

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(Long studentId, Long courseId, Long assignmentId,
            AssignmentSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        courseAccessService.requireActiveStudent(student.getId(), assignment.getCourse());
        referenceService.validateResult(assignment, studentId, request.getResultRefId());

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

        AssignmentSubmission saved = submissionRepository.save(submission);
        if (assignment.getTeacher() != null) {
            boolean late = saved.getStatus() == AssignmentSubmissionStatus.LATE;
            notificationOutboxService.enqueue(assignment.getTeacher().getId(), NotificationType.ASSIGNMENT,
                    late ? "Bài nộp trễ" : "Bài nộp mới",
                    student.getFullName() + " đã nộp bài \"" + assignment.getTitle() + "\"" + (late ? " (sau hạn nộp)." : "."),
                    "submission-received:" + saved.getId() + ":" + submittedAt);
        }
        return classroomMapper.toAssignmentSubmissionResponse(saved);
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(Long teacherId, Long courseId, Long assignmentId,
            Long submissionId, GradeSubmissionRequest request) {
        assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        AssignmentSubmission submission = submissionRepository.findByIdForUpdate(submissionId)
                .orElseThrow(() -> ErrorCode.SUBMISSION_NOT_FOUND.toException());
        if (!submission.getAssignment().getId().equals(assignmentId)
                || !submission.getAssignment().getCourse().getId().equals(courseId)) {
            throw ErrorCode.SUBMISSION_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseTeacher(teacherId, submission.getAssignment().getCourse());
        if (request.getStatus() != AssignmentSubmissionStatus.GRADED || request.getScore() == null
                || request.getScore().signum() < 0 || request.getScore().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        submission.setScore(request.getScore());
        submission.setGradingRevision(submission.getGradingRevision() + 1);
        submission.setTeacherCommentText(request.getTeacherCommentText());
        submission.setTeacherAudioCommentUrl(request.getTeacherAudioCommentUrl());
        submission.setStatus(request.getStatus());
        submission.setCommentedAt(LocalDateTime.now());

        AssignmentSubmission savedSubmission = submissionRepository.save(submission);

        notificationOutboxService.enqueue(savedSubmission.getStudent().getId(), NotificationType.GRADE,
            "Đã có điểm", 
            "Bài tập " + savedSubmission.getAssignment().getTitle() + " đã được chấm điểm.", 
            "submission-graded:" + savedSubmission.getId() + ":" + savedSubmission.getGradingRevision());

        return classroomMapper.toAssignmentSubmissionResponse(savedSubmission);
    }

    public PageResponse<AssignmentSubmissionResponse> getSubmissionsByAssignment(Long teacherId, Long courseId,
            Long assignmentId, AssignmentSubmissionStatus status, Pageable pageable) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseTeacher(teacherId, assignment.getCourse());
        Page<AssignmentSubmission> pageResult = status == null
                ? submissionRepository.findAllByAssignmentId(assignmentId, pageable)
                : submissionRepository.findAllByAssignmentIdAndStatus(assignmentId, status, pageable);
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

    public AssignmentSubmissionDetailResponse getSubmissionDetail(Long teacherId, Long courseId, Long assignmentId,
            Long submissionId) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> ErrorCode.SUBMISSION_NOT_FOUND.toException());
        Assignment assignment = submission.getAssignment();
        if (!assignment.getId().equals(assignmentId) || !assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.SUBMISSION_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseTeacher(teacherId, assignment.getCourse());
        return AssignmentSubmissionDetailResponse.builder()
                .submission(classroomMapper.toAssignmentSubmissionResponse(submission))
                .result(referenceService.describeResult(assignment.getModuleType(), submission.getResultRefId())
                        .orElse(null))
                .build();
    }

    /** The student's own submission with the learning result behind it; teacher score/comments only once GRADED. */
    public AssignmentSubmissionDetailResponse getMySubmission(Long studentId, Long courseId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> ErrorCode.SUBMISSION_NOT_FOUND.toException());
        AssignmentSubmissionResponse response = classroomMapper.toAssignmentSubmissionResponse(submission);
        if (submission.getStatus() != AssignmentSubmissionStatus.GRADED) {
            response.setScore(null);
            response.setTeacherCommentText(null);
            response.setTeacherAudioCommentUrl(null);
            response.setCommentedAt(null);
        }
        return AssignmentSubmissionDetailResponse.builder()
                .submission(response)
                .result(referenceService.describeResult(assignment.getModuleType(), submission.getResultRefId())
                        .orElse(null))
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
