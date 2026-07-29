package com.example.english_app.service.classroom;

import com.example.english_app.dto.request.classroom.CourseStudentRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.dto.response.classroom.CourseResponse;
import com.example.english_app.dto.response.classroom.CourseStudentDetailResponse;
import com.example.english_app.dto.response.classroom.CourseStudentResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseStudentService {

    private final CourseStudentRepository courseStudentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final StudentStatRepository studentStatRepository;
    private final ClassroomMapper classroomMapper;

    public CourseStudentResponse addStudentToCourse(Long courseId, CourseStudentRequest request) {

        if (courseStudentRepository.existsByCourseIdAndStudentId(courseId, request.getStudentId())) {
            throw ErrorCode.STUDENT_ALREADY_IN_COURSE.toException();
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        CourseStudent courseStudent = CourseStudent.builder()
                .course(course)
                .student(student)
                .status(ClassStudentStatus.ACTIVE)
                .build();

        return classroomMapper.toCourseStudentResponse(courseStudentRepository.save((courseStudent)));
    }

    public void removeStudentFromCourse(Long courseId, Long studentId) {
        CourseStudent courseStudent = courseStudentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new RuntimeException("Student is not enrolled in this course"));
        courseStudentRepository.delete(courseStudent);
    }

    public PageResponse<CourseStudentDetailResponse> getStudentsByCourseWithStats(Long courseId, String keyword,
            ClassStudentStatus status, Pageable pageable) {
        Page<CourseStudent> coursePage = courseStudentRepository.filterStudentsInCourse(courseId, keyword, status,
                pageable);

        List<CourseStudentDetailResponse> content = coursePage.getContent().stream().map(cs -> {
            User student = cs.getStudent();
            StudentStat stat = studentStatRepository.findById(student.getId()).orElse(null);

            UserResponse userInfo = UserResponse.builder()
                    .id(student.getId())
                    .email(student.getEmail())
                    .fullName(student.getFullName())
                    .avatarUrl(student.getAvatarUrl())
                    .build();

            StudentStatResponse statInfo = null;
            if (stat != null) {
                statInfo = StudentStatResponse.builder()
                        .totalXp(stat.getTotalXp())
                        .currentStreak(stat.getCurrentStreak())
                        .totalStudyMinutes(stat.getTotalStudyMinutes())
                        .build();
            }

            return classroomMapper.toCourseStudentDetailResponse(cs, userInfo, statInfo);
        }).collect(Collectors.toList());

        return PageResponse.<CourseStudentDetailResponse>builder()
                .content(content)
                .pageSize(coursePage.getSize())
                .currentPage(coursePage.getNumber() + 1)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    public PageResponse<CourseResponse> getCoursesByStudent(Long studentId, Pageable pageable) {
        Page<CourseStudent> coursePage = courseStudentRepository.findAllByStudentId(studentId, pageable);

        List<CourseResponse> content = coursePage.getContent().stream()
                .map(cs -> classroomMapper.toCourseResponse(cs.getCourse()))
                .collect(Collectors.toList());

        return PageResponse.<CourseResponse>builder()
                .content(content)
                .pageSize(coursePage.getSize())
                .currentPage(coursePage.getNumber() + 1)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }
}
