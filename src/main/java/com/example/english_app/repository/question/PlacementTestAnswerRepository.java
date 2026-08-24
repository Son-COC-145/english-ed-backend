package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlacementTestAnswerRepository extends JpaRepository<PlacementTestAnswer, Long> {

    @EntityGraph(attributePaths = { "question" })
    List<PlacementTestAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    @Query("SELECT a.question.id FROM PlacementTestAnswer a WHERE a.session.id = :sessionId")
    List<Long> findAnsweredQuestionIdsBySessionId(@Param("sessionId") Long sessionId);

    long countBySessionId(Long sessionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
