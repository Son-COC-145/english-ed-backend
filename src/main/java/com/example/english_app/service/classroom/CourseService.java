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
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final ClassroomMapper classroomMapper;

    public CourseResponse createCourse(CourseRequest request) {
        User teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacher.getRole() != Role.TEACHER) {
            throw new RuntimeException("User is not a teacher");
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

    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

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
                    .orElseThrow(() -> new RuntimeException("Teacher not found"));
            course.setTeacher(teacher);
        }
        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    public CourseResponse activate(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setIsActive(true);

        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    public CourseResponse deactivate(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        course.setIsActive(false);

        return classroomMapper.toCourseResponse(courseRepository.save(course));
    }

    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
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