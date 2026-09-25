package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlacementTestAnswerRepository extends JpaRepository<PlacementTestAnswer, Long> {

    @EntityGraph(attributePaths = { "question" })
    List<PlacementTestAnswer> findBySessionIdOrderByAnsweredAtAsc(Long sessionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);

    @EntityGraph(attributePaths = { "question" })
    Optional<PlacementTestAnswer> findBySessionIdAndSubmissionId(Long sessionId, UUID submissionId);

    @EntityGraph(attributePaths = { "question" })
    Optional<PlacementTestAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    @Query("""
            select a from PlacementTestAnswer a
            join fetch a.question q
            where a.session.id = :sessionId
              and (a.submissionId = :submissionId or q.id = :questionId)
            order by a.id
            """)
    List<PlacementTestAnswer> findReplayCandidates(
            @Param("sessionId") Long sessionId,
            @Param("submissionId") UUID submissionId,
            @Param("questionId") Long questionId);
}
