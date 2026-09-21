package com.example.english_app.entity.speaking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "speaking_reward_ledger")
@Getter
@NoArgsConstructor
public class SpeakingRewardLedger {
    @Id
    @Column(name = "session_id")
    private Long sessionId;
    @Column(name = "student_id", nullable = false)
    private Long studentId;
    @Column(name = "xp_amount", nullable = false)
    private short xpAmount;
}
