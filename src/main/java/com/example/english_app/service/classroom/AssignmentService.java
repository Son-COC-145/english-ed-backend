package com.example.english_app.service.classroom;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.classroom.AssignmentRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentResponse;
import com.example.english_app.dto.response.classroom.AssignmentSubmissionStatsResponse;
import com.example.english_app.dto.response.classroom.MySubmissionResponse;
import com.example.english_app.entity.classroom.AssignmentSubmission;
import com.example.english_app.entity.enums.StudentAssignmentFilter;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.service.notification.NotificationOutboxService;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final ClassroomMapper classroomMapper;
    private final CourseStudentRepository courseStudentRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final CourseAccessService courseAccessService;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentReferenceService referenceService;

    @Transactional
    public AssignmentResponse createAssignment(Long teacherId, Long courseId, AssignmentRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        courseAccessService.requireCourseTeacher(teacherId, course);
        referenceService.validateTarget(request.getModuleType(), request.getRefId());
        User teacher = course.getTeacher();

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .course(course)
                .teacher(teacher)
                .description(request.getDescription())
                .moduleType(request.getModuleType())
                .refId(request.getRefId())
                .deadlineAt(request.getDeadlineAt())
                .build();

        Assignment savedAssignment = assignmentRepository.save(assignment);

        List<CourseStudent> students = courseStudentRepository.findByCourseIdAndStatus(courseId, ClassStudentStatus.ACTIVE);
        for (CourseStudent cs : students) {
             notificationOutboxService.enqueue(cs.getStudent().getId(), NotificationType.ASSIGNMENT,
                "Bài tập mới", 
                "Giáo viên vừa giao bài tập: " + savedAssignment.getTitle(), 
                "assignment-created:" + savedAssignment.getId() + ":" + cs.getStudent().getId());
        }

        return classroomMapper.toAssignmentResponse(savedAssignment);
    }

    public PageResponse<AssignmentResponse> getAssignmentsForTeacher(Long teacherId, Long courseId, String keyword,
            Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireCourseTeacher(teacherId, course);
        Page<Assignment> pageResult = assignmentRepository.findAllByCourseIdWithKeyword(courseId, keyword, pageable);
        long activeStudents = courseStudentRepository.countByCourseIdAndStatus(courseId, ClassStudentStatus.ACTIVE);
        Map<Long, Map<AssignmentSubmissionStatus, Long>> counts = countSubmissions(pageResult.getContent());
        return toPageResponse(pageResult, assignment -> {
            AssignmentResponse response = classroomMapper.toAssignmentResponse(assignment);
            response.setSubmissionStats(buildStats(activeStudents,
                    counts.getOrDefault(assignment.getId(), Map.of())));
            return response;
        });
    }

    public PageResponse<AssignmentResponse> getAssignmentsForStudent(Long studentId, Long courseId, String keyword,
            Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireActiveStudent(studentId, course);
        Page<Assignment> pageResult = assignmentRepository.findAllByCourseIdWithKeyword(courseId, keyword, pageable);
        return toStudentPage(studentId, pageResult, false);
    }

    /** Assignments of every course the student is ACTIVE in, nearest deadline first, filtered by the student's progress. */
    public PageResponse<AssignmentResponse> getStudentAssignments(Long studentId, StudentAssignmentFilter filter,
            Pageable pageable) {
        Page<Assignment> pageResult = switch (filter == null ? StudentAssignmentFilter.ALL : filter) {
            case ALL -> assignmentRepository.findAllForStudent(studentId, pageable);
            case TODO -> assignmentRepository.findNotSubmittedForStudent(studentId, pageable);
            case SUBMITTED -> assignmentRepository.findForStudentBySubmissionStatus(studentId,
                    EnumSet.of(AssignmentSubmissionStatus.SUBMITTED, AssignmentSubmissionStatus.LATE), pageable);
            case GRADED -> assignmentRepository.findForStudentBySubmissionStatus(studentId,
                    EnumSet.of(AssignmentSubmissionStatus.GRADED), pageable);
        };
        return toStudentPage(studentId, pageResult, true);
    }

    private PageResponse<AssignmentResponse> toStudentPage(Long studentId, Page<Assignment> pageResult,
            boolean withCourseName) {
        List<Long> ids = pageResult.getContent().stream().map(Assignment::getId).toList();
        Map<Long, AssignmentSubmission> mine = ids.isEmpty() ? Map.of()
                : submissionRepository.findAllByStudentIdAndAssignmentIdIn(studentId, ids).stream()
                        .collect(Collectors.toMap(submission -> submission.getAssignment().getId(), Function.identity()));
        return toPageResponse(pageResult, assignment -> {
            AssignmentResponse response = classroomMapper.toAssignmentResponse(assignment);
            response.setMySubmission(toMySubmission(mine.get(assignment.getId())));
            if (withCourseName) {
                response.setCourseName(assignment.getCourse().getName());
            }
            return response;
        });
    }

    private MySubmissionResponse toMySubmission(AssignmentSubmission submission) {
        if (submission == null) {
            return null;
        }
        boolean graded = submission.getStatus() == AssignmentSubmissionStatus.GRADED;
        return MySubmissionResponse.builder()
                .id(submission.getId())
                .status(submission.getStatus())
                .score(graded ? submission.getScore() : null)
                .submittedAt(submission.getSubmittedAt())
                .commentedAt(submission.getCommentedAt())
                .hasTeacherComment(graded && (submission.getTeacherCommentText() != null
                        || submission.getTeacherAudioCommentUrl() != null))
                .build();
    }

    private Map<Long, Map<AssignmentSubmissionStatus, Long>> countSubmissions(List<Assignment> assignments) {
        if (assignments.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = assignments.stream().map(Assignment::getId).toList();
        Map<Long, Map<AssignmentSubmissionStatus, Long>> counts = new HashMap<>();
        for (AssignmentSubmissionRepository.SubmissionStatusCount row
                : submissionRepository.countByAssignmentIdsAndStatus(ids, ClassStudentStatus.ACTIVE)) {
            counts.computeIfAbsent(row.getAssignmentId(), id -> new EnumMap<>(AssignmentSubmissionStatus.class))
                    .put(row.getStatus(), row.getTotal());
        }
        return counts;
    }

    private AssignmentSubmissionStatsResponse buildStats(long activeStudents,
            Map<AssignmentSubmissionStatus, Long> byStatus) {
        long submitted = byStatus.getOrDefault(AssignmentSubmissionStatus.SUBMITTED, 0L);
        long late = byStatus.getOrDefault(AssignmentSubmissionStatus.LATE, 0L);
        long graded = byStatus.getOrDefault(AssignmentSubmissionStatus.GRADED, 0L);
        return AssignmentSubmissionStatsResponse.builder()
                .activeStudents(activeStudents)
                .submittedCount(submitted + late + graded)
                .lateCount(late)
                .gradedCount(graded)
                .notSubmittedCount(Math.max(0L, activeStudents - submitted - late - graded))
                .build();
    }

    private PageResponse<AssignmentResponse> toPageResponse(Page<Assignment> pageResult,
            Function<Assignment, AssignmentResponse> mapper) {
        List<AssignmentResponse> content = pageResult.getContent().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return PageResponse.<AssignmentResponse>builder()
                .content(content)
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long teacherId, Long courseId, Long assignmentId, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseTeacher(teacherId, assignment.getCourse());
        referenceService.validateTarget(request.getModuleType(), request.getRefId());
        if ((assignment.getModuleType() != request.getModuleType() || !assignment.getRefId().equals(request.getRefId()))
                && submissionRepository.existsByAssignmentId(assignmentId)) {
            throw ErrorCode.ASSIGNMENT_HAS_SUBMISSIONS.toException();
        }
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setModuleType(request.getModuleType());
        assignment.setRefId(request.getRefId());
        assignment.setDeadlineAt(request.getDeadlineAt());
        return classroomMapper.toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(Long teacherId, Long courseId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findByIdForUpdate(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseTeacher(teacherId, assignment.getCourse());
        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw ErrorCode.ASSIGNMENT_HAS_SUBMISSIONS.toException();
        }
        assignmentRepository.delete(assignment);
    }
}
