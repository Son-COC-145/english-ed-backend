package com.example.english_app.entity.speaking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "speaking_jobs")
@Getter
@NoArgsConstructor
public class SpeakingJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    @Column(name = "turn_id")
    private Long turnId;
    private String kind;
    private String status;
    private int attempts;
    @Column(name = "max_attempts")
    private int maxAttempts;
}
