package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.TodayPlan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.util.Optional;
import java.time.LocalDateTime;

public interface TodayPlanRepository extends JpaRepository<TodayPlan, Long> {

    Optional<TodayPlan> findByStudentIdAndPlanDateAndTimezone(
            Long studentId, LocalDate planDate, String timezone);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM TodayPlan p
            WHERE p.studentId = :studentId
              AND p.planDate = :planDate
              AND p.timezone = :timezone
            """)
    Optional<TodayPlan> findForUpdate(
            @Param("studentId") Long studentId,
            @Param("planDate") LocalDate planDate,
            @Param("timezone") String timezone);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE TodayPlan p SET p.dirty = true, p.updatedAt = :now
            WHERE p.studentId = :studentId
              AND p.planDate >= :fromDate
            """)
    int markDirty(
            @Param("studentId") Long studentId,
            @Param("fromDate") LocalDate fromDate,
            @Param("now") LocalDateTime now);
}
