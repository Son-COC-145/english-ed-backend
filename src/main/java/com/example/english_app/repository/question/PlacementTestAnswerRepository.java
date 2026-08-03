package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;

public interface PlacementTestAnswerRepository extends JpaRepository<PlacementTestAnswer, Long> {

    @EntityGraph(attributePaths = {"question"})
    List<PlacementTestAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
