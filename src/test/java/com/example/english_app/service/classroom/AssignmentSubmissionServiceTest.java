package com.example.english_app.service.classroom;

import com.example.english_app.dto.request.classroom.AssignmentSubmissionRequest;
import com.example.english_app.dto.request.classroom.GradeSubmissionRequest;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionDetailResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionResponse;
import com.example.english_app.dto.response.classroom.SubmissionResultResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.AssignmentSubmission;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.notification.NotificationOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionServiceTest {
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private NotificationOutboxService notificationOutboxService;
    @Mock private CourseAccessService courseAccessService;
    @Mock private AssignmentReferenceService referenceService;
    @InjectMocks private AssignmentSubmissionService service;

    @Test
    void submitRejectsAssignmentFromAnotherCourse() {
        Assignment assignment = assignment(3L, 10L);
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment));

        AppException error = assertThrows(AppException.class, () -> service.submitAssignment(7L, 99L, 3L,
                AssignmentSubmissionRequest.builder().resultRefId(1L).build()));

        assertEquals(ErrorCode.ASSIGNMENT_NOT_FOUND, error.getErrorCode());
        verifyNoInteractions(userRepository, submissionRepository, courseAccessService);
    }

    @Test
    void submitAfterDeadlineMarksSubmissionLate() {
        Assignment assignment = assignment(3L, 10L);
        assignment.setDeadlineAt(LocalDateTime.now().minusMinutes(1));
        User student = User.builder().id(7L).build();
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment));
        when(userRepository.findById(7L)).thenReturn(Optional.of(student));
        when(submissionRepository.findByAssignmentIdAndStudentId(3L, 7L)).thenReturn(Optional.empty());
        when(submissionRepository.save(any(AssignmentSubmission.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classroomMapper.toAssignmentSubmissionResponse(any())).thenReturn(AssignmentSubmissionResponse.builder().build());

        service.submitAssignment(7L, 10L, 3L, AssignmentSubmissionRequest.builder().resultRefId(8L).build());

        verify(courseAccessService).requireActiveStudent(7L, assignment.getCourse());
        verify(submissionRepository).save(argThat(submission -> submission.getStatus() == AssignmentSubmissionStatus.LATE
                && submission.getResultRefId().equals(8L)));
    }

    @Test
    void gradePersistsAudioAndEnqueuesIdempotentEvent() {
        Assignment assignment = assignment(3L, 10L);
        AssignmentSubmission submission = AssignmentSubmission.builder().id(4L).assignment(assignment)
                .student(User.builder().id(7L).build()).status(AssignmentSubmissionStatus.SUBMITTED).build();
        GradeSubmissionRequest request = GradeSubmissionRequest.builder().score(new BigDecimal("90"))
                .status(AssignmentSubmissionStatus.GRADED).teacherAudioCommentUrl("https://audio.test/comment.mp3").build();
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(classroomMapper.toAssignmentSubmissionResponse(any())).thenReturn(AssignmentSubmissionResponse.builder().build());

        service.gradeSubmission(1L, 10L, 3L, 4L, request);

        assertEquals("https://audio.test/comment.mp3", submission.getTeacherAudioCommentUrl());
        assertEquals(AssignmentSubmissionStatus.GRADED, submission.getStatus());
        verify(notificationOutboxService).enqueue(eq(7L), any(), any(), any(), eq("submission-graded:4:1"));
    }

    @Test
    void teacherCanReviseGradeAndEachRevisionHasItsOwnNotificationKey() {
        Assignment assignment = assignment(3L,10L);
        AssignmentSubmission submission = AssignmentSubmission.builder().id(4L).assignment(assignment)
                .student(User.builder().id(7L).build()).status(AssignmentSubmissionStatus.GRADED).gradingRevision(1L).build();
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findByIdForUpdate(4L)).thenReturn(Optional.of(submission));
        when(submissionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.gradeSubmission(1L,10L,3L,4L,GradeSubmissionRequest.builder()
                .status(AssignmentSubmissionStatus.GRADED).score(new BigDecimal("95")).build());
        assertEquals(new BigDecimal("95"),submission.getScore());
        assertEquals(2L,submission.getGradingRevision());
        verify(notificationOutboxService).enqueue(eq(7L),any(),any(),any(),eq("submission-graded:4:2"));
    }

    @Test
    void listSubmissionsFiltersByStatusWhenRequested() {
        Assignment assignment = assignment(3L, 10L);
        Pageable pageable = PageRequest.of(0, 10);
        when(assignmentRepository.findById(3L)).thenReturn(Optional.of(assignment));
        when(submissionRepository.findAllByAssignmentIdAndStatus(3L, AssignmentSubmissionStatus.LATE, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.getSubmissionsByAssignment(1L, 10L, 3L, AssignmentSubmissionStatus.LATE, pageable);

        verify(courseAccessService).requireCourseTeacher(1L, assignment.getCourse());
        verify(submissionRepository, never()).findAllByAssignmentId(any(), any());
    }

    @Test
    void submissionDetailRejectsSubmissionFromAnotherAssignment() {
        AssignmentSubmission submission = AssignmentSubmission.builder().id(4L).assignment(assignment(5L, 10L))
                .student(User.builder().id(7L).build()).build();
        when(submissionRepository.findById(4L)).thenReturn(Optional.of(submission));

        AppException error = assertThrows(AppException.class, () -> service.getSubmissionDetail(1L, 10L, 3L, 4L));

        assertEquals(ErrorCode.SUBMISSION_NOT_FOUND, error.getErrorCode());
        verifyNoInteractions(courseAccessService, referenceService);
    }

    @Test
    void submissionDetailIncludesLearningResult() {
        Assignment assignment = assignment(3L, 10L);
        AssignmentSubmission submission = AssignmentSubmission.builder().id(4L).assignment(assignment)
                .student(User.builder().id(7L).build()).resultRefId(8L).build();
        SubmissionResultResponse result = SubmissionResultResponse.builder()
                .moduleType(ModuleType.VOCABULARY).resultId(8L).overallScore(80).build();
        when(submissionRepository.findById(4L)).thenReturn(Optional.of(submission));
        when(referenceService.describeResult(ModuleType.VOCABULARY, 8L)).thenReturn(Optional.of(result));
        when(classroomMapper.toAssignmentSubmissionResponse(submission))
                .thenReturn(AssignmentSubmissionResponse.builder().id(4L).build());

        AssignmentSubmissionDetailResponse detail = service.getSubmissionDetail(1L, 10L, 3L, 4L);

        verify(courseAccessService).requireCourseTeacher(1L, assignment.getCourse());
        assertEquals(4L, detail.getSubmission().getId());
        assertSame(result, detail.getResult());
    }

    private Assignment assignment(Long assignmentId, Long courseId) {
        return Assignment.builder().id(assignmentId).moduleType(ModuleType.VOCABULARY).refId(1L)
                .title("Unit test").course(Course.builder().id(courseId).isActive(true).build()).build();
    }
}
