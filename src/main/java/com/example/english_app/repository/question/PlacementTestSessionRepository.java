package com.example.english_app.repository.question;

import com.example.english_app.entity.onboarding.PlacementTestSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacementTestSessionRepository extends JpaRepository<PlacementTestSession, Long> {

    Optional<PlacementTestSession> findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(Long studentId);

    Optional<PlacementTestSession> findTopByStudentIdOrderByStartedAtDesc(Long studentId);
}

