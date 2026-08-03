package com.example.english_app.repository.gamification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.english_app.entity.gamification.StudentStat;

@Repository
public interface StudentStatRepository extends JpaRepository<StudentStat, Long> {
    Optional<StudentStat> findByStudentId(Long studentId);
}
