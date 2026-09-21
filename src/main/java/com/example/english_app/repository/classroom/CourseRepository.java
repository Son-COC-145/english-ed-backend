package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.Course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Course c where c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM class_students WHERE class_id=:id) " +
            "OR EXISTS(SELECT 1 FROM assignments WHERE class_id=:id) " +
            "OR EXISTS(SELECT 1 FROM teaching_materials WHERE class_id=:id) " +
            "OR EXISTS(SELECT 1 FROM syllabus_items WHERE class_id=:id)", nativeQuery = true)
    boolean hasDependentData(@Param("id") Long id);

    Page<Course> findAllByTeacherId(Long teacherId, Pageable pageable);

    List<Course> findAllByIsActiveTrue();
}
