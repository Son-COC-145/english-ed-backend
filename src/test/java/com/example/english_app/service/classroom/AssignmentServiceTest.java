package com.example.english_app.service.classroom;

import com.example.english_app.dto.request.classroom.AssignmentRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionStatsResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.service.notification.NotificationOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private CourseStudentRepository courseStudentRepository;
    @Mock private NotificationOutboxService notificationOutboxService;
    @Mock private CourseAccessService courseAccessService;
    @Mock private AssignmentSubmissionRepository submissionRepository;
    @Mock private AssignmentReferenceService referenceService;
    @InjectMocks private AssignmentService service;

    @Test
    void teacherListIncludesSubmissionStatsOfActiveStudents() {
        Course course = Course.builder().id(10L).build();
        Assignment first = assignment(3L, course);
        Assignment second = assignment(4L, course);
        Pageable pageable = PageRequest.of(0, 10);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(assignmentRepository.findAllByCourseIdWithKeyword(10L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(courseStudentRepository.countByCourseIdAndStatus(10L, ClassStudentStatus.ACTIVE)).thenReturn(5L);
        when(submissionRepository.countByAssignmentIdsAndStatus(List.of(3L, 4L), ClassStudentStatus.ACTIVE))
                .thenReturn(List.of(
                        count(3L, AssignmentSubmissionStatus.SUBMITTED, 1L),
                        count(3L, AssignmentSubmissionStatus.LATE, 1L),
                        count(3L, AssignmentSubmissionStatus.GRADED, 2L)));
        when(classroomMapper.toAssignmentResponse(any()))
                .thenAnswer(invocation -> AssignmentResponse.builder()
                        .id(invocation.<Assignment>getArgument(0).getId()).build());

        PageResponse<AssignmentResponse> page = service.getAssignmentsForTeacher(1L, 10L, null, pageable);

        verify(courseAccessService).requireCourseTeacher(1L, course);
        AssignmentSubmissionStatsResponse stats = page.getContent().get(0).getSubmissionStats();
        assertEquals(5L, stats.getActiveStudents());
        assertEquals(4L, stats.getSubmittedCount());
        assertEquals(1L, stats.getLateCount());
        assertEquals(2L, stats.getGradedCount());
        assertEquals(1L, stats.getNotSubmittedCount());
        AssignmentSubmissionStatsResponse empty = page.getContent().get(1).getSubmissionStats();
        assertEquals(0L, empty.getSubmittedCount());
        assertEquals(5L, empty.getNotSubmittedCount());
    }

    @Test
    void deleteAssignmentWithSubmissionsReturnsDedicatedConflict() {
        Course course = Course.builder().id(10L).build();
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment(3L, course)));
        when(submissionRepository.existsByAssignmentId(3L)).thenReturn(true);

        AppException error = assertThrows(AppException.class, () -> service.deleteAssignment(1L, 10L, 3L));

        assertEquals(ErrorCode.ASSIGNMENT_HAS_SUBMISSIONS, error.getErrorCode());
        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    void changingTargetOfAssignmentWithSubmissionsIsRejected() {
        Course course = Course.builder().id(10L).build();
        when(assignmentRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(assignment(3L, course)));
        when(submissionRepository.existsByAssignmentId(3L)).thenReturn(true);
        AssignmentRequest request = AssignmentRequest.builder().title("Updated")
                .moduleType(ModuleType.SPEAKING).refId(2L).build();

        AppException error = assertThrows(AppException.class, () -> service.updateAssignment(1L, 10L, 3L, request));

        assertEquals(ErrorCode.ASSIGNMENT_HAS_SUBMISSIONS, error.getErrorCode());
        verify(assignmentRepository, never()).save(any());
    }

    private Assignment assignment(Long id, Course course) {
        return Assignment.builder().id(id).course(course).title("Unit " + id)
                .moduleType(ModuleType.VOCABULARY).refId(1L).build();
    }

    private AssignmentSubmissionRepository.SubmissionStatusCount count(Long assignmentId,
            AssignmentSubmissionStatus status, Long total) {
        return new AssignmentSubmissionRepository.SubmissionStatusCount() {
            @Override public Long getAssignmentId() { return assignmentId; }
            @Override public AssignmentSubmissionStatus getStatus() { return status; }
            @Override public Long getTotal() { return total; }
        };
    }
}
