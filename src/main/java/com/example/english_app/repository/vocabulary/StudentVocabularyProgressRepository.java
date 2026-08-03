package com.example.english_app.repository.vocabulary;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;

@Repository
public interface StudentVocabularyProgressRepository extends JpaRepository<StudentVocabularyProgress, Long> {
    Optional<StudentVocabularyProgress> findByStudentIdAndVocabularyId(Long studentId, Long vocabularyId);
    
    Optional<StudentVocabularyProgress> findByIdAndStudentId(Long id, Long studentId);
    
    Page<StudentVocabularyProgress> findByStudentId(Long studentId, Pageable pageable);
}
