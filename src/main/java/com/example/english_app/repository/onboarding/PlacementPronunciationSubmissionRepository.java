package com.example.english_app.repository.onboarding;

import com.example.english_app.entity.onboarding.PlacementPronunciationSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PlacementPronunciationSubmissionRepository
        extends JpaRepository<PlacementPronunciationSubmission, Long> {

    Optional<PlacementPronunciationSubmission> findBySessionIdAndSubmissionId(
            Long sessionId, UUID submissionId);

    Optional<PlacementPronunciationSubmission> findBySessionIdAndQuestionId(
            Long sessionId, Long questionId);

    @Modifying
    @Query(value = """
            INSERT INTO placement_pronunciation_submissions
                (session_id, question_id, submission_id, request_hash, status, created_at, updated_at)
            VALUES (:sessionId, :questionId, :submissionId, :requestHash,
                    'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertClaim(
            @Param("sessionId") Long sessionId,
            @Param("questionId") Long questionId,
            @Param("submissionId") UUID submissionId,
            @Param("requestHash") String requestHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update PlacementPronunciationSubmission s
               set s.updatedAt = :now
             where s.sessionId = :sessionId
               and s.questionId = :questionId
               and s.submissionId = :submissionId
               and s.requestHash = :requestHash
               and s.status = 'PROCESSING'
               and s.updatedAt < :staleBefore
            """)
    int reclaimStale(
            @Param("sessionId") Long sessionId,
            @Param("questionId") Long questionId,
            @Param("submissionId") UUID submissionId,
            @Param("requestHash") String requestHash,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            delete from PlacementPronunciationSubmission s
             where s.sessionId = :sessionId
               and s.submissionId = :submissionId
               and s.requestHash = :requestHash
               and s.status = 'PROCESSING'
            """)
    int release(
            @Param("sessionId") Long sessionId,
            @Param("submissionId") UUID submissionId,
            @Param("requestHash") String requestHash);

    @Modifying
    @Query("""
            update PlacementPronunciationSubmission s
               set s.status = 'COMPLETED', s.updatedAt = :now
             where s.sessionId = :sessionId
               and s.submissionId = :submissionId
               and s.requestHash = :requestHash
            """)
    int complete(
            @Param("sessionId") Long sessionId,
            @Param("submissionId") UUID submissionId,
            @Param("requestHash") String requestHash,
            @Param("now") LocalDateTime now);
}
