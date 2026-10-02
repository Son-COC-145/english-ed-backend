package com.example.english_app.entity.adaptive;

import com.example.english_app.entity.enums.RoadmapModuleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "roadmap_module_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roadmap_module_progress",
                columnNames = {"student_id", "roadmap_version", "module_key"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapModuleProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "roadmap_version", nullable = false)
    private Integer roadmapVersion;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "module_index", nullable = false)
    private Integer moduleIndex;

    @Column(name = "module_key", nullable = false, length = 120)
    private String moduleKey;

    @Column(name = "module_type", nullable = false, length = 40)
    private String moduleType;

    @Column(name = "topic_id")
    private Short topicId;

    @Column(name = "done_count", nullable = false)
    private Integer doneCount;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoadmapModuleStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
