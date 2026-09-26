package com.example.english_app.service.classroom;

import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.example.english_app.dto.request.classroom.CourseRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.CourseResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.exception.ErrorCode;
import org.springframework.transaction.annotation.Transactional;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final ClassroomMapper classroomMapper;
    private final CourseAccessService courseAccessService;

    /** One course for its teacher or an admin (course detail/edit pages). */
    public CourseResponse getCourse(Long actorId, Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireTeacherOrAdmin(actorId, course);
        return classroomMapper.toCourseResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        if (teacher.getRole() != Role.TEACHER) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        Course course = Course.builder()
                .name(request.getName())
                .teacher(teacher)
                .description(request.getDescription())
                .cefrTarget(request.getCefrTarget())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        if (request.getName() != null)
            course.setName(request.getName());
        if (request.getDescription() != null)
            course.setDescription(request.getDescription());
        if (request.getCefrTarget() != null)
            course.setCefrTarget(request.getCefrTarget());
        if (request.getIsActive() != null)
            course.setIsActive(request.getIsActive());
        if (request.getStartDate() != null)
            course.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            course.setEndDate(request.getEndDate());

        if (request.getTeacherId() != null) {
            User teacher = userRepository.findById(request.getTeacherId())
                    .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
            if (teacher.getRole() != Role.TEACHER) throw ErrorCode.INVALID_REQUEST.toException();
            course.setTeacher(teacher);
        }
        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse activate(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        course.setIsActive(true);

        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse deactivate(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        if (courseStudentRepository.existsByCourseId(id)) {
            throw ErrorCode.COURSE_HAS_STUDENTS.toException();
        }

        course.setIsActive(false);

        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        if (courseRepository.hasDependentData(id)) {
            throw ErrorCode.CLASSROOM_RESOURCE_IN_USE.toException();
        }
        courseRepository.delete(course);
    }

    public PageResponse<CourseResponse> getAllCourses(Pageable pageable) {
        Page<Course> coursePage = courseRepository.findAll(pageable);

        return PageResponse.<CourseResponse>builder()
                .content(coursePage.getContent().stream().map(classroomMapper::toCourseResponse)
                        .collect(Collectors.toList()))
                .pageSize(coursePage.getSize())
                .currentPage(coursePage.getNumber() + 1)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    public PageResponse<CourseResponse> getCoursesByTeacher(Long teacherId, Pageable pageable) {
        Page<Course> coursePage = courseRepository.findAllByTeacherId(teacherId, pageable);

        return PageResponse.<CourseResponse>builder()
                .content(coursePage.getContent()
                        .stream()
                        .map(classroomMapper::toCourseResponse)
                        .collect(Collectors.toList()))
                .pageSize(coursePage.getSize())
                .currentPage(coursePage.getNumber() + 1)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

}
