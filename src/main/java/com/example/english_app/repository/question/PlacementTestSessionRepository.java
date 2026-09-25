package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.time.LocalDateTime;

public interface PlacementTestSessionRepository extends JpaRepository<PlacementTestSession, Long> {

    Optional<PlacementTestSession> findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM PlacementTestSession s
            WHERE s.student.id = :studentId AND s.isCompleted = false
            ORDER BY s.startedAt DESC
            """)
    Optional<PlacementTestSession> findActiveByStudentIdForUpdate(@Param("studentId") Long studentId);

    Optional<PlacementTestSession> findTopByStudentIdOrderByStartedAtDesc(Long studentId);

    @Query("SELECT s FROM PlacementTestSession s JOIN FETCH s.student WHERE s.id = :id")
    Optional<PlacementTestSession> findByIdWithStudent(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM PlacementTestSession s JOIN FETCH s.student WHERE s.id = :id")
    Optional<PlacementTestSession> findByIdWithStudentForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE placement_test_sessions
            SET is_completed = true,
                current_question_id = NULL,
                last_activity_at = :now
            WHERE id = :sessionId
              AND student_id = :userId
              AND is_completed = false
              AND COALESCE(last_activity_at, started_at) < :cutoff
            """, nativeQuery = true)
    int closeIfExpired(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("now") LocalDateTime now);
}
