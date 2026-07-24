package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByCourseId(Long courseId);
    List<Assignment> findAllByTeacherId(Long teacherId);
}
