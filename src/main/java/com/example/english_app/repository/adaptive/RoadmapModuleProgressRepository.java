package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoadmapModuleProgressRepository extends JpaRepository<RoadmapModuleProgress, Long> {

    List<RoadmapModuleProgress> findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
            Long studentId, Integer roadmapVersion);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO roadmap_module_progress (
                student_id, roadmap_version, week_number, module_index,
                module_key, module_type, topic_id, done_count, total_count,
                status, completed_at, updated_at)
            VALUES (
                :studentId, :roadmapVersion, :weekNumber, :moduleIndex,
                :moduleKey, :moduleType, :topicId, :doneCount, :totalCount,
                :status, :completedAt, :updatedAt)
            ON CONFLICT (student_id, roadmap_version, module_key)
            DO UPDATE SET
                week_number = EXCLUDED.week_number,
                module_index = EXCLUDED.module_index,
                module_type = EXCLUDED.module_type,
                topic_id = EXCLUDED.topic_id,
                done_count = LEAST(
                    roadmap_module_progress.total_count,
                    GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count)),
                total_count = roadmap_module_progress.total_count,
                status = CASE
                    WHEN roadmap_module_progress.status = 'COMPLETED' THEN 'COMPLETED'
                    WHEN LEAST(
                            roadmap_module_progress.total_count,
                            GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count))
                         = roadmap_module_progress.total_count THEN 'COMPLETED'
                    WHEN GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count) > 0
                         THEN 'IN_PROGRESS'
                    ELSE 'NOT_STARTED'
                END,
                completed_at = CASE
                    WHEN roadmap_module_progress.completed_at IS NOT NULL
                         THEN roadmap_module_progress.completed_at
                    WHEN LEAST(
                            roadmap_module_progress.total_count,
                            GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count))
                         = roadmap_module_progress.total_count THEN EXCLUDED.updated_at
                    ELSE NULL
                END,
                updated_at = EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsert(
            @Param("studentId") Long studentId,
            @Param("roadmapVersion") Integer roadmapVersion,
            @Param("weekNumber") Integer weekNumber,
            @Param("moduleIndex") Integer moduleIndex,
            @Param("moduleKey") String moduleKey,
            @Param("moduleType") String moduleType,
            @Param("topicId") Short topicId,
            @Param("doneCount") Integer doneCount,
            @Param("totalCount") Integer totalCount,
            @Param("status") String status,
            @Param("completedAt") LocalDateTime completedAt,
            @Param("updatedAt") LocalDateTime updatedAt);
}
