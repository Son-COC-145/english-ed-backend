package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpeakingTurnRepository extends JpaRepository<SpeakingTurn, Long> {

    List<SpeakingTurn> findBySessionIdOrderByTurnIndexAscIdAsc(Long sessionId);

    Optional<SpeakingTurn> findBySessionIdAndRequestKey(Long sessionId, String requestKey);
}
