package com.example.english_app.repository.gamification;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.english_app.entity.vocabulary.MinigameResult;

@Repository
public interface MinigameResultRepository extends JpaRepository<MinigameResult, Long> {
    Optional<MinigameResult> findByIdAndStudentId(Long id, Long studentId);
    
    Page<MinigameResult> findByStudentId(Long studentId, Pageable pageable);
}
