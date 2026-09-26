package com.example.english_app.service.classroom;

import com.example.english_app.dto.request.classroom.UpdateCourseStudentStatusRequest;
import com.example.english_app.dto.response.classroom.CourseStudentResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseStudentServiceTest {
    @Mock private CourseStudentRepository courseStudentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentStatRepository studentStatRepository;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private CourseAccessService courseAccessService;
    @InjectMocks private CourseStudentService service;

    @Test
    void updateStatusSuspendsEnrollment() {
        Course course = Course.builder().id(10L).build();
        CourseStudent enrollment = CourseStudent.builder().id(5L).course(course).status(ClassStudentStatus.ACTIVE).build();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseStudentRepository.findByCourseIdAndStudentId(10L, 7L)).thenReturn(Optional.of(enrollment));
        when(courseStudentRepository.save(any(CourseStudent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(classroomMapper.toCourseStudentResponse(any())).thenReturn(CourseStudentResponse.builder().build());

        service.updateStudentStatus(1L, 10L, 7L, UpdateCourseStudentStatusRequest.builder()
                .status(ClassStudentStatus.INACTIVE).build());

        verify(courseAccessService).requireTeacherOrAdmin(1L, course);
        assertEquals(ClassStudentStatus.INACTIVE, enrollment.getStatus());
    }

    @Test
    void blankCandidateKeywordListsAllStudentsNotYetEnrolled() {
        Course course = Course.builder().id(10L).build();
        Pageable pageable = PageRequest.of(0, 20);
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(userRepository.searchEnrollmentCandidates(null, Role.STUDENT, 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(User.builder().id(7L).fullName("An").email("an@test.vn").build()), pageable, 1));

        PageResponse<UserResponse> result = service.findStudentCandidates(1L, 10L, "   ", pageable);

        verify(courseAccessService).requireTeacherOrAdmin(1L, course);
        assertEquals(1, result.getContent().size());
        assertEquals(7L, result.getContent().get(0).getId());
    }

    @Test
    void updateStatusRejectsStudentOutsideCourse() {
        Course course = Course.builder().id(10L).build();
        when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        when(courseStudentRepository.findByCourseIdAndStudentId(10L, 7L)).thenReturn(Optional.empty());

        AppException error = assertThrows(AppException.class, () -> service.updateStudentStatus(1L, 10L, 7L,
                UpdateCourseStudentStatusRequest.builder().status(ClassStudentStatus.ACTIVE).build()));

        assertEquals(ErrorCode.STUDENT_NOT_IN_COURSE, error.getErrorCode());
        verify(courseStudentRepository, never()).save(any());
    }
}
