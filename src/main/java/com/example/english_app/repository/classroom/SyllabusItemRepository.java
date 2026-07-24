package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.SyllabusItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyllabusItemRepository extends JpaRepository<SyllabusItem, Long> {
    List<SyllabusItem> findAllByCourseId(Long courseId);
}
