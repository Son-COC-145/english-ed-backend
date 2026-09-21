package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CourseServiceTest {
    @Test void courseWithDependentDataCannotBeDeleted() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseStudentRepository courseStudentRepository = mock(CourseStudentRepository.class);
        CourseService service = new CourseService(mock(UserRepository.class), repository, courseStudentRepository, mock(ClassroomMapper.class));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(Course.builder().id(1L).build()));
        when(repository.hasDependentData(1L)).thenReturn(true);
        AppException error = assertThrows(AppException.class, () -> service.deleteCourse(1L));
        assertEquals(ErrorCode.CLASSROOM_RESOURCE_IN_USE, error.getErrorCode());
        verify(repository, never()).delete(any());
    }

    @Test void courseWithStudentsCannotBeDeactivated() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseStudentRepository courseStudentRepository = mock(CourseStudentRepository.class);
        CourseService service = new CourseService(mock(UserRepository.class), repository, courseStudentRepository, mock(ClassroomMapper.class));
        when(repository.findById(1L)).thenReturn(Optional.of(Course.builder().id(1L).isActive(true).build()));
        when(courseStudentRepository.existsByCourseId(1L)).thenReturn(true);

        AppException error = assertThrows(AppException.class, () -> service.deactivate(1L));
        assertEquals(ErrorCode.COURSE_HAS_STUDENTS, error.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test void courseWithoutStudentsCanBeDeactivated() {
        CourseRepository repository = mock(CourseRepository.class);
        CourseStudentRepository courseStudentRepository = mock(CourseStudentRepository.class);
        ClassroomMapper mapper = mock(ClassroomMapper.class);
        CourseService service = new CourseService(mock(UserRepository.class), repository, courseStudentRepository, mapper);
        Course course = Course.builder().id(1L).isActive(true).build();
        when(repository.findById(1L)).thenReturn(Optional.of(course));
        when(courseStudentRepository.existsByCourseId(1L)).thenReturn(false);
        when(repository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deactivate(1L);
        assertFalse(course.getIsActive());
        verify(repository).save(course);
    }
}
