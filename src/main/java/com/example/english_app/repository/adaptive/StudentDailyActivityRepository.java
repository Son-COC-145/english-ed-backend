package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.StudentDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface StudentDailyActivityRepository extends JpaRepository<StudentDailyActivity, Long> {

    Optional<StudentDailyActivity> findByStudentIdAndActivityDate(Long studentId, LocalDate activityDate);

    @Modifying
    @Query(value = """
            INSERT INTO student_daily_activity (
                student_id, activity_date, measured_seconds,
                estimated_seconds, activity_count, updated_at)
            VALUES (
                :studentId, :activityDate, :measuredSeconds,
                :estimatedSeconds, 1, :updatedAt)
            ON CONFLICT (student_id, activity_date)
            DO UPDATE SET
                measured_seconds = student_daily_activity.measured_seconds + EXCLUDED.measured_seconds,
                estimated_seconds = student_daily_activity.estimated_seconds + EXCLUDED.estimated_seconds,
                activity_count = student_daily_activity.activity_count + 1,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int accumulate(
            @Param("studentId") Long studentId,
            @Param("activityDate") LocalDate activityDate,
            @Param("measuredSeconds") int measuredSeconds,
            @Param("estimatedSeconds") int estimatedSeconds,
            @Param("updatedAt") LocalDateTime updatedAt);
}
