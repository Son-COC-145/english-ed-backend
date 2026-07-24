package com.example.english_app.repository;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.speaking.SpeakingScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeakingScenarioRepository extends JpaRepository<SpeakingScenario, Short> {
    
    List<SpeakingScenario> findByCefrLevelAndTopicIdInAndIsActiveTrue(
            CefrLevel cefrLevel, List<Short> topicIds);
}
