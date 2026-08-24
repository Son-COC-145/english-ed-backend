package com.example.english_app.repository.onboarding;

import com.example.english_app.entity.onboarding.StudentOnboarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OnboardingRepository extends JpaRepository<StudentOnboarding, Long> {

    Optional<StudentOnboarding> findByStudentId(Long studentId);

    @Query("SELECT ob FROM StudentOnboarding ob JOIN FETCH ob.student WHERE ob.student.id = :userId")
    Optional<StudentOnboarding> findByStudentIdWithUser(@Param("userId") Long userId);

    boolean existsByStudentId(Long studentId);
}
