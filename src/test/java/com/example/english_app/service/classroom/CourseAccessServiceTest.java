package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseAccessServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private CourseStudentRepository courseStudentRepository;
    @InjectMocks private CourseAccessService accessService;

    @Test
    void rejectsTeacherWhoDoesNotOwnCourse() {
        Course course = Course.builder().id(10L)
                .teacher(User.builder().id(1L).role(Role.TEACHER).build()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).role(Role.TEACHER).build()));

        AppException error = assertThrows(AppException.class,
                () -> accessService.requireTeacherOrAdmin(2L, course));

        assertEquals(ErrorCode.COURSE_ACCESS_DENIED, error.getErrorCode());
    }

    @Test
    void allowsAdminToManageAnyCourse() {
        Course course = Course.builder().id(10L)
                .teacher(User.builder().id(1L).role(Role.TEACHER).build()).build();
        User admin = User.builder().id(2L).role(Role.ADMIN).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        assertSame(admin, accessService.requireTeacherOrAdmin(2L, course));
    }

    @Test
    void courseTeacherCheckRejectsAdmin() {
        Course course = Course.builder().id(10L)
                .teacher(User.builder().id(1L).role(Role.TEACHER).build()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).role(Role.ADMIN).build()));

        AppException error = assertThrows(AppException.class,
                () -> accessService.requireCourseTeacher(2L, course));

        assertEquals(ErrorCode.COURSE_ACCESS_DENIED, error.getErrorCode());
    }

    @Test
    void courseTeacherCheckRejectsTeacherOfAnotherCourse() {
        Course course = Course.builder().id(10L)
                .teacher(User.builder().id(1L).role(Role.TEACHER).build()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).role(Role.TEACHER).build()));

        AppException error = assertThrows(AppException.class,
                () -> accessService.requireCourseTeacher(2L, course));

        assertEquals(ErrorCode.COURSE_ACCESS_DENIED, error.getErrorCode());
    }

    @Test
    void courseTeacherCheckAllowsOwner() {
        User owner = User.builder().id(1L).role(Role.TEACHER).build();
        Course course = Course.builder().id(10L).teacher(owner).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertSame(owner, accessService.requireCourseTeacher(1L, course));
    }

    @Test
    void rejectsInactiveEnrollment() {
        Course course = Course.builder().id(10L).isActive(true).build();
        when(courseStudentRepository.findByCourseIdAndStudentId(10L, 7L))
                .thenReturn(Optional.of(CourseStudent.builder().status(ClassStudentStatus.INACTIVE).build()));

        AppException error = assertThrows(AppException.class,
                () -> accessService.requireActiveStudent(7L, course));

        assertEquals(ErrorCode.STUDENT_NOT_IN_COURSE, error.getErrorCode());
    }
}
