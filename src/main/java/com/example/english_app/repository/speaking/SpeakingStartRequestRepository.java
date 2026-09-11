package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingStartRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpeakingStartRequestRepository extends JpaRepository<SpeakingStartRequest, Long> {

    @Modifying
    @Query(value = "INSERT INTO speaking_start_requests (student_id, request_key, scenario_id) "
            + "VALUES (:studentId, :requestKey, :scenarioId) "
            + "ON CONFLICT (student_id, request_key) DO NOTHING", nativeQuery = true)
    int claim(@Param("studentId") Long studentId,
              @Param("requestKey") String requestKey,
              @Param("scenarioId") Short scenarioId);

    @Query("SELECT r FROM SpeakingStartRequest r "
            + "WHERE r.student.id = :studentId AND r.requestKey = :requestKey")
    Optional<SpeakingStartRequest> findByStudentIdAndRequestKey(@Param("studentId") Long studentId,
                                                                 @Param("requestKey") String requestKey);

    @Modifying
    @Query("UPDATE SpeakingStartRequest r SET r.session = :session "
            + "WHERE r.student.id = :studentId AND r.requestKey = :requestKey")
    int bindSession(@Param("studentId") Long studentId,
                    @Param("requestKey") String requestKey,
                    @Param("session") SpeakingSession session);
}
