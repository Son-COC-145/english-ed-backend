package com.example.english_app.entity.classroom;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseStudentId implements Serializable {

    @Column(name = "class_id")
    private Long courseId;

    @Column(name = "student_id")
    private Long studentId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseStudentId that = (CourseStudentId) o;
        return Objects.equals(courseId, that.courseId) &&
               Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courseId, studentId);
    }
}
