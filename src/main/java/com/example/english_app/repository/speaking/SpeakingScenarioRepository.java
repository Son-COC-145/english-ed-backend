package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingScenario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpeakingScenarioRepository extends JpaRepository<SpeakingScenario, Short> {

    boolean existsByTitleVi(String titleVi);
    boolean existsByTitleEn(String titleEn);

    boolean existsByTitleViAndIdNot(String titleVi, Short id);
    boolean existsByTitleEnAndIdNot(String titleEn, Short id);

    @Query("SELECT s FROM SpeakingScenario s WHERE " +
           "(:id IS NULL OR s.id = :id) AND " +
           "(:topicId IS NULL OR s.topic.id = :topicId) AND " +
           "(:isActive IS NULL OR s.isActive = :isActive) AND " +
           "(:title IS NULL OR LOWER(s.titleVi) LIKE LOWER(CONCAT('%', :title, '%')) " +
           "OR LOWER(s.titleEn) LIKE LOWER(CONCAT('%', :title, '%')))")
    Page<SpeakingScenario> filterScenarios(
            @Param("id") Short id,
            @Param("title") String title,
            @Param("topicId") Short topicId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
