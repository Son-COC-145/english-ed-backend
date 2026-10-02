package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {
    List<SpeakingSession> findTop20ByStudentIdAndStatusInOrderByStartedAtDescIdDesc(
            Long studentId, Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SpeakingSession s WHERE s.id = :id")
    Optional<SpeakingSession> lockById(@Param("id") Long id);

    @Query("SELECT COUNT(DISTINCT s.scenario.id) FROM SpeakingSession s " +
           "WHERE s.student.id = :studentId " +
           "AND s.status = 'COMPLETED' " +
           "AND s.scenario.id IN :scenarioIds")
    long countCompletedScenarioIds(
            @Param("studentId") Long studentId,
            @Param("scenarioIds") List<Short> scenarioIds);
}
