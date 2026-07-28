package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.SyllabusItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyllabusItemRepository extends JpaRepository<SyllabusItem, Long> {
    List<SyllabusItem> findAllByCourseId(Long courseId);

    @Query("SELECT s FROM SyllabusItem s WHERE s.course.id = :courseId " +
            "AND (:keyword IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) " +
            "AND (:weekNumber IS NULL OR s.weekNumber = :weekNumber)")
    Page<SyllabusItem> findAllByCourseIdWithFilters(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("weekNumber") Short weekNumber,
            Pageable pageable);
}
