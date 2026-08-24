package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlacementTestSessionRepository extends JpaRepository<PlacementTestSession, Long> {

    Optional<PlacementTestSession> findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(Long studentId);

    Optional<PlacementTestSession> findTopByStudentIdOrderByStartedAtDesc(Long studentId);

    @Query("SELECT s FROM PlacementTestSession s JOIN FETCH s.student WHERE s.id = :id")
    Optional<PlacementTestSession> findByIdWithStudent(@Param("id") Long id);
}
