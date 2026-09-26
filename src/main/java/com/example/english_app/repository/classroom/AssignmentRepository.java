package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.Assignment;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Assignment a where a.id = :id")
    Optional<Assignment> findByIdForUpdate(@Param("id") Long id);
    
    List<Assignment> findAllByCourseId(Long courseId);

    List<Assignment> findAllByTeacherId(Long teacherId);

    @Query("SELECT a FROM Assignment a WHERE a.course.id = :courseId " +
            "AND(:keyword IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))")
    Page<Assignment> findAllByCourseIdWithKeyword(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            Pageable pageable);

    String STUDENT_SCOPE = "FROM Assignment a JOIN a.course c WHERE c.isActive = true " +
            "AND EXISTS (SELECT 1 FROM CourseStudent cs WHERE cs.course = c AND cs.student.id = :studentId " +
            "AND cs.status = com.example.english_app.entity.enums.ClassStudentStatus.ACTIVE) ";
    String NEAREST_DEADLINE_FIRST = " ORDER BY a.deadlineAt ASC NULLS LAST, a.id DESC";

    /** Assignments of every active course the student is ACTIVE in, nearest deadline first. */
    @Query(value = "SELECT a " + STUDENT_SCOPE + NEAREST_DEADLINE_FIRST,
            countQuery = "SELECT COUNT(a) " + STUDENT_SCOPE)
    Page<Assignment> findAllForStudent(@Param("studentId") Long studentId, Pageable pageable);

    /** Same scope, only assignments the student has not submitted yet. */
    @Query(value = "SELECT a " + STUDENT_SCOPE + "AND NOT EXISTS (SELECT 1 FROM AssignmentSubmission s " +
            "WHERE s.assignment = a AND s.student.id = :studentId)" + NEAREST_DEADLINE_FIRST,
            countQuery = "SELECT COUNT(a) " + STUDENT_SCOPE + "AND NOT EXISTS (SELECT 1 FROM AssignmentSubmission s " +
                    "WHERE s.assignment = a AND s.student.id = :studentId)")
    Page<Assignment> findNotSubmittedForStudent(@Param("studentId") Long studentId, Pageable pageable);

    /** Same scope, only assignments whose submission by the student has one of the given statuses. */
    @Query(value = "SELECT a " + STUDENT_SCOPE + "AND EXISTS (SELECT 1 FROM AssignmentSubmission s " +
            "WHERE s.assignment = a AND s.student.id = :studentId AND s.status IN :statuses)" + NEAREST_DEADLINE_FIRST,
            countQuery = "SELECT COUNT(a) " + STUDENT_SCOPE + "AND EXISTS (SELECT 1 FROM AssignmentSubmission s " +
                    "WHERE s.assignment = a AND s.student.id = :studentId AND s.status IN :statuses)")
    Page<Assignment> findForStudentBySubmissionStatus(@Param("studentId") Long studentId,
            @Param("statuses") java.util.Collection<com.example.english_app.entity.enums.AssignmentSubmissionStatus> statuses,
            Pageable pageable);
}
