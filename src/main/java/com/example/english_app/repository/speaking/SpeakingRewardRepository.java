package com.example.english_app.repository.speaking;

import com.example.english_app.entity.speaking.SpeakingRewardLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

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

    /**
     * Counts a learning day for the streak, atomically and with the same rule as
     * {@code GameficationService.updateStreakLogic}: already active today → unchanged, active yesterday → +1,
     * otherwise → 1. Native so it does not overwrite the XP just added by {@link #incrementXp}.
     * The student_stats row must exist (call after incrementXp).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            update student_stats set
                current_streak = case
                    when last_activity_date = :today then current_streak
                    when last_activity_date = CAST(:today AS date) - 1 then current_streak + 1
                    else 1 end,
                longest_streak = greatest(longest_streak, case
                    when last_activity_date = :today then current_streak
                    when last_activity_date = CAST(:today AS date) - 1 then current_streak + 1
                    else 1 end),
                last_activity_date = :today,
                updated_at = CURRENT_TIMESTAMP
            where student_id = :studentId
            """, nativeQuery = true)
    int recordActivityDay(@Param("studentId") Long studentId, @Param("today") LocalDate today);

}
