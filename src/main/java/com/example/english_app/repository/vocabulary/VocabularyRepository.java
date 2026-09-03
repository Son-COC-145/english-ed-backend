package com.example.english_app.repository.vocabulary;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.vocabulary.Vocabulary;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {
    boolean existsByWord(String word);
    
    long countByTopicId(Short topicId);

    @EntityGraph(attributePaths = {"topic", "createdBy"})
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

    @Query(value = """
        SELECT v.* FROM vocabularies v
        WHERE v.topic_id IN (:topicIds)
        AND NOT EXISTS (
            SELECT 1 FROM student_vocabulary_progress svp 
            WHERE svp.vocabulary_id = v.id AND svp.student_id = :studentId
        )
        ORDER BY RANDOM()
        LIMIT :limit
        """, nativeQuery = true)
    java.util.List<Vocabulary> findRandomNewVocabularies(
        @Param("topicIds") java.util.List<Long> topicIds, 
        @Param("studentId") Long studentId, 
        @Param("limit") int limit);
}

