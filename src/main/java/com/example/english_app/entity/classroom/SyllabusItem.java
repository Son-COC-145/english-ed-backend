package com.example.english_app.entity.classroom;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "syllabus_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Course course;

    @Column(name = "week_number", nullable = false)
    private Short weekNumber;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private TeachingMaterial material;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Short sortOrder = 0;
}
