package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingRewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpeakingRewardRepository extends JpaRepository<SpeakingRewardLedger, Long> {
    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into speaking_reward_ledger(session_id, student_id, xp_amount)
            values (:sessionId,:studentId,:xp) on conflict (session_id) do nothing
            """, nativeQuery = true)
    int insertOnce(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId, @Param("xp") short xp);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into student_stats(student_id, total_xp, current_streak, longest_streak,
                streak_freeze_count, total_study_minutes, updated_at)
            values (:studentId,:xp,0,0,0,0,CURRENT_TIMESTAMP)
            on conflict (student_id) do update set
                total_xp = student_stats.total_xp + EXCLUDED.total_xp, updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void incrementXp(@Param("studentId") Long studentId, @Param("xp") short xp);

}
