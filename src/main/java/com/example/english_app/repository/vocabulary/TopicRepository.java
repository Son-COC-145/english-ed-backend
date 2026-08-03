package com.example.english_app.repository.vocabulary;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.vocabulary.Topic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface TopicRepository extends JpaRepository<Topic, Short> {
    List<Topic> findByNameEnContainingIgnoreCase(String nameEn);

    List<Topic> findByNameViContainingIgnoreCase(String nameVi);

    boolean existsByNameEn(String nameEn);

    boolean existsByNameVi(String nameVi);

    @Query("SELECT t FROM Topic t WHERE " +
        "(:isActive IS NULL OR t.isActive = :isActive) AND " +
        "(:nameSearch IS NULL OR LOWER(t.nameEn) LIKE LOWER(CONCAT('%', CAST(:nameSearch AS string), '%')) " +
        "OR LOWER(t.nameVi) LIKE LOWER(CONCAT('%', CAST(:nameSearch AS string), '%')))")
    Page<Topic> filterTopics(
            @Param("nameSearch") String nameSearch,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    @Query("SELECT t FROM Topic t WHERE t.isActive = true " +
           "AND (t.cefrLevel = :cefrLevel OR t.cefrLevel IS NULL) " +
           "AND (:categories IS NULL OR t.category IN :categories) " +
           "ORDER BY CASE WHEN t.cefrLevel = :cefrLevel THEN 0 ELSE 1 END")
    List<Topic> findForRoadmap(@Param("cefrLevel") CefrLevel cefrLevel,
                               @Param("categories") List<String> categories,
                               Pageable pageable);
}

