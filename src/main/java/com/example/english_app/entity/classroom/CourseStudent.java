package com.example.english_app.entity.classroom;

import com.example.english_app.entity.enums.ClassStudentStatus;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "class_students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStudent {

    @EmbeddedId
    private CourseStudentId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseId")
    @JoinColumn(name = "class_id")
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private User student;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ClassStudentStatus status = ClassStudentStatus.ACTIVE;
}
