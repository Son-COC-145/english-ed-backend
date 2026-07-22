package com.example.english_app.repository.speaking;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.english_app.entity.speaking.SpeakingSession;

public interface SpeakingSessionRepository extends JpaRepository<SpeakingSession, Long> {

}
