package com.example.english_app.repository;

import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementTestAnswerRepository extends JpaRepository<PlacementTestAnswer, Long> {

    List<PlacementTestAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
