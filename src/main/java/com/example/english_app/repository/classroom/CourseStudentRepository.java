package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.CourseStudent;
import com.example.english_app.entity.enums.ClassStudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseStudentRepository extends JpaRepository<CourseStudent, Long> {
    @EntityGraph(attributePaths = {"student"})
    @Query("SELECT cs FROM CourseStudent cs " +
            "WHERE cs.course.id = :courseId " +
            "AND (:status IS NULL OR cs.status = :status) " +
            "AND (:keyword IS NULL OR " +
            "     LOWER(cs.student.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
            "     LOWER(cs.student.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))")
    Page<CourseStudent> filterStudentsInCourse(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("status") ClassStudentStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"course"})
    @Query("SELECT cs FROM CourseStudent cs WHERE cs.student.id = :studentId " +
            "AND cs.status = :status AND cs.course.isActive = true")
    Page<CourseStudent> findAllByStudentIdAndStatus(@Param("studentId") Long studentId,
            @Param("status") ClassStudentStatus status, Pageable pageable);

    Optional<CourseStudent> findByCourseIdAndStudentId(Long courseId, Long studentId);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    List<CourseStudent> findByCourseId(Long courseId);

    List<CourseStudent> findByCourseIdAndStatus(Long courseId, ClassStudentStatus status);

    @Query("SELECT cs.course FROM CourseStudent cs " +
           "WHERE cs.student.id = :studentId AND cs.status = 'ACTIVE'")
    List<com.example.english_app.entity.classroom.Course> findActiveCoursesByStudentId(
           @Param("studentId") Long studentId);
}
