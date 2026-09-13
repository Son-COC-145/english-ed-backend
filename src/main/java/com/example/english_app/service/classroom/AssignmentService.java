package com.example.english_app.service.classroom;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.classroom.AssignmentRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.AssignmentResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.AssignmentRepository;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.service.notification.NotificationOutboxService;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.enums.NotificationType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final ClassroomMapper classroomMapper;
    private final CourseStudentRepository courseStudentRepository;
    private final NotificationOutboxService notificationOutboxService;
    private final CourseAccessService courseAccessService;

    @Transactional
    public AssignmentResponse createAssignment(Long teacherId, Long courseId, AssignmentRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        courseAccessService.requireTeacherOrAdmin(teacherId, course);
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
        courseAccessService.requireTeacherOrAdmin(teacherId, course);
        return getAssignments(courseId, keyword, pageable);
    }

    public PageResponse<AssignmentResponse> getAssignmentsForStudent(Long studentId, Long courseId, String keyword,
            Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireActiveStudent(studentId, course);
        return getAssignments(courseId, keyword, pageable);
    }

    private PageResponse<AssignmentResponse> getAssignments(Long courseId, String keyword, Pageable pageable) {
        Page<Assignment> pageResult = assignmentRepository.findAllByCourseIdWithKeyword(courseId, keyword, pageable);
        List<AssignmentResponse> content = pageResult.getContent().stream()
                .map(classroomMapper::toAssignmentResponse)
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
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, assignment.getCourse());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setModuleType(request.getModuleType());
        assignment.setRefId(request.getRefId());
        assignment.setDeadlineAt(request.getDeadlineAt());
        return classroomMapper.toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(Long teacherId, Long courseId, Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        if (!assignment.getCourse().getId().equals(courseId)) {
            throw ErrorCode.ASSIGNMENT_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, assignment.getCourse());
        assignmentRepository.delete(assignment);
    }
}
