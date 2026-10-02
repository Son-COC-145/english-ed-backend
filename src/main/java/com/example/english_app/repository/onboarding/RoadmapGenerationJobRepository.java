package com.example.english_app.repository.onboarding;

import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoadmapGenerationJobRepository extends JpaRepository<RoadmapGenerationJob, Long> {

    boolean existsByStudentIdAndGenerationVersion(Long studentId, Integer generationVersion);

    Optional<RoadmapGenerationJob> findTopByStudentIdOrderByGenerationVersionDesc(Long studentId);

    @Query(value = """
            SELECT * FROM roadmap_generation_jobs
            WHERE (status = 'PENDING' AND available_at <= :now)
               OR (status = 'PROCESSING' AND locked_at < :staleBefore)
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<RoadmapGenerationJob> lockDispatchable(
            @Param("limit") int limit,
            @Param("now") LocalDateTime now,
            @Param("staleBefore") LocalDateTime staleBefore);

    @Modifying
    @Query("""
            update RoadmapGenerationJob j
               set j.status = :status,
                   j.lockedAt = null,
                   j.claimToken = null,
                   j.lastError = :error,
                   j.attemptCount = :attempts,
                   j.availableAt = :availableAt,
                   j.updatedAt = CURRENT_TIMESTAMP
             where j.id = :id
               and j.status = com.example.english_app.entity.enums.RoadmapGenerationStatus.PROCESSING
               and j.claimToken = :claimToken
            """)
    int completeClaim(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("status") RoadmapGenerationStatus status,
            @Param("error") String error,
            @Param("attempts") int attempts,
            @Param("availableAt") LocalDateTime availableAt);
}
