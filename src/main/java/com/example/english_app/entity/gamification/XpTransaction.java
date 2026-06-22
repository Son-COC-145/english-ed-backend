package com.example.english_app.entity.gamification;

import com.example.english_app.entity.enums.XpSourceType;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "xp_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "xp_amount", nullable = false)
    private Short xpAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private XpSourceType sourceType;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
