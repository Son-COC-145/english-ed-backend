package com.example.english_app.repository;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.vocabulary.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    boolean existsByWord(String word);
    
    long countByTopicId(Short topicId);

    @Query("SELECT v FROM Vocabulary v WHERE " +
        "(:topicId IS NULL OR v.topic.id = :topicId) AND " +
        "(:createdById IS NULL OR v.createdBy.id = :createdById) AND " + 
        "(:status IS NULL OR v.status = :status) AND " +
        "(:cefrLevel IS NULL OR v.cefrLevel = :cefrLevel) AND " +
        "(:wordSearch IS NULL OR LOWER(v.word) LIKE LOWER(CONCAT('%', CAST(:wordSearch AS string), '%')))")
    Page<Vocabulary> filterVocabularies(
            @Param("topicId") Short topicId,
            @Param("createdById") Long createdById,
            @Param("status") VocabularyStatus status,
            @Param("cefrLevel") CefrLevel cefrLevel,
            @Param("wordSearch") String wordSearch,
            Pageable pageable);
}
