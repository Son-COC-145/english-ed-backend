package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.Assignment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findAllByCourseId(Long courseId);

    List<Assignment> findAllByTeacherId(Long teacherId);

    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId " +
            "AND(:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Assignment> findAllByCourseIdWithKeyword(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
