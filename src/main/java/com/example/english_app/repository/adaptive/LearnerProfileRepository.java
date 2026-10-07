package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.LearnerProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LearnerProfileRepository extends JpaRepository<LearnerProfile, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM LearnerProfile p WHERE p.studentId = :studentId")
    Optional<LearnerProfile> findByStudentIdForUpdate(@Param("studentId") Long studentId);
}
