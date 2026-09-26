package com.example.english_app.repository.gamification;

import com.example.english_app.entity.vocabulary.MinigameRound;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MinigameRoundRepository extends JpaRepository<MinigameRound, Long> {
    Optional<MinigameRound> findByIdAndStudentId(Long id, Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from MinigameRound r where r.id = :id and r.student.id = :studentId")
    Optional<MinigameRound> findByIdAndStudentIdForUpdate(@Param("id") Long id, @Param("studentId") Long studentId);
}
