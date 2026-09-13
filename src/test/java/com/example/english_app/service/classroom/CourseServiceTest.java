package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.repository.classroom.CourseRepository;
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
        CourseService service = new CourseService(mock(UserRepository.class),repository,mock(ClassroomMapper.class));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(Course.builder().id(1L).build()));
        when(repository.hasDependentData(1L)).thenReturn(true);
        AppException error = assertThrows(AppException.class, () -> service.deleteCourse(1L));
        assertEquals(ErrorCode.CLASSROOM_RESOURCE_IN_USE,error.getErrorCode());
        verify(repository,never()).delete(any());
    }
}
