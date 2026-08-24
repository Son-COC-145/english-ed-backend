package com.example.english_app.repository.vocabulary;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.entity.vocabulary.Vocabulary;

@Repository
public interface StudentVocabularyProgressRepository extends JpaRepository<StudentVocabularyProgress, Long> {
    Optional<StudentVocabularyProgress> findByStudentIdAndVocabularyId(Long studentId, Long vocabularyId);
    
    Optional<StudentVocabularyProgress> findByIdAndStudentId(Long id, Long studentId);
    
    Page<StudentVocabularyProgress> findByStudentId(Long studentId, Pageable pageable);

    @Query(value = "SELECT v FROM StudentVocabularyProgress p " +
           "JOIN p.vocabulary v " +
           "LEFT JOIN FETCH v.topic t " +
           "LEFT JOIN FETCH v.createdBy c " +
           "WHERE p.student.id = :studentId " +
           "AND p.nextReviewAt <= :now ",
           countQuery = "SELECT COUNT(p) FROM StudentVocabularyProgress p WHERE p.student.id = :studentId AND p.nextReviewAt <= :now")
    Page<Vocabulary> findVocabulariesToReview(
           @Param("studentId") Long studentId, 
           @Param("now") LocalDateTime now, 
           Pageable pageable);
}

