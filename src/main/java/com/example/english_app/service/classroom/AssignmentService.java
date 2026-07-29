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
import com.example.english_app.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssignmentService {
    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ClassroomMapper classroomMapper;

    @Transactional
    public AssignmentResponse createAssignment(Long teacherId, Long courseId, AssignmentRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .course(course)
                .teacher(teacher)
                .description(request.getDescription())
                .moduleType(request.getModuleType())
                .refId(request.getRefId())
                .deadlineAt(request.getDeadlineAt())
                .build();

        return classroomMapper.toAssignmentResponse(assignmentRepository.save(assignment));
    }

    public PageResponse<AssignmentResponse> getAssignmentsByCourse(Long courseId, String keyword, Pageable pageable) {
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
    public AssignmentResponse updateAssignment(Long assignmentId, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> ErrorCode.ASSIGNMENT_NOT_FOUND.toException());
        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setModuleType(request.getModuleType());
        assignment.setRefId(request.getRefId());
        assignment.setDeadlineAt(request.getDeadlineAt());
        return classroomMapper.toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Transactional
    public void deleteAssignment(Long assignmentId) {
        assignmentRepository.deleteById(assignmentId);
    }
}
