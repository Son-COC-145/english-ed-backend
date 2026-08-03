package com.example.english_app.repository.gamification;

import com.example.english_app.entity.gamification.DailyGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyGoalRepository extends JpaRepository<DailyGoal, Long> {

    Optional<DailyGoal> findByStudentIdAndGoalDate(Long studentId, LocalDate goalDate);

    boolean existsByStudentIdAndGoalDate(Long studentId, LocalDate goalDate);
}
