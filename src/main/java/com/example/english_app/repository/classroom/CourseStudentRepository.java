package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.CourseStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseStudentRepository extends JpaRepository<CourseStudent, Long> {
    List<CourseStudent> findAllByCourseId(Long courseId);
    List<CourseStudent> findAllByStudentId(Long studentId);
    Optional<CourseStudent> findByCourseIdAndStudentId(Long courseId, Long studentId);
}
