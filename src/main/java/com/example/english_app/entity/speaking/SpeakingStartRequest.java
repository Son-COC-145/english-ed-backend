package com.example.english_app.entity.speaking;

import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speaking_start_requests",
        uniqueConstraints = @UniqueConstraint(name = "uq_speaking_start_request",
                columnNames = {"student_id", "request_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingStartRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "request_key", nullable = false, length = 100)
    private String requestKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private SpeakingScenario scenario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SpeakingSession session;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
