package com.example.english_app.repository.onboarding;

import com.example.english_app.entity.onboarding.StudentOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<StudentOnboarding, Long> {

    Optional<StudentOnboarding> findByStudentId(Long studentId);

    boolean existsByStudentId(Long studentId);
}

