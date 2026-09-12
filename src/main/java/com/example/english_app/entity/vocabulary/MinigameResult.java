package com.example.english_app.entity.vocabulary;

import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "minigame_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinigameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type", nullable = false)
    private GameType gameType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(nullable = false)
    private Short score;

    @Column(name = "xp_earned", nullable = false)
    private Short xpEarned;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;
}
