package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findAllByTeacherId(Long teacherId);
    List<Course> findAllByIsActiveTrue();
}
