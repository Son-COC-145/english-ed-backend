package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.AssignmentSubmission;
import com.example.english_app.entity.enums.AssignmentSubmissionStatus;
import com.example.english_app.entity.enums.ClassStudentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    boolean existsByAssignmentId(Long assignmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AssignmentSubmission s where s.id = :id")
    Optional<AssignmentSubmission> findByIdForUpdate(@Param("id") Long id);
    
    @EntityGraph(attributePaths = {"student"})
    Page<AssignmentSubmission> findAllByAssignmentId(Long assignmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"student"})
    Page<AssignmentSubmission> findAllByAssignmentIdAndStatus(Long assignmentId, AssignmentSubmissionStatus status,
            Pageable pageable);

    /** Counts submissions per assignment and status, only for students whose enrollment has the given status. */
    @Query("SELECT s.assignment.id AS assignmentId, s.status AS status, COUNT(s) AS total " +
            "FROM AssignmentSubmission s WHERE s.assignment.id IN :assignmentIds " +
            "AND EXISTS (SELECT 1 FROM CourseStudent cs WHERE cs.course.id = s.assignment.course.id " +
            "AND cs.student.id = s.student.id AND cs.status = :enrollmentStatus) " +
            "GROUP BY s.assignment.id, s.status")
    List<SubmissionStatusCount> countByAssignmentIdsAndStatus(
            @Param("assignmentIds") Collection<Long> assignmentIds,
            @Param("enrollmentStatus") ClassStudentStatus enrollmentStatus);

    interface SubmissionStatusCount {
        Long getAssignmentId();

        AssignmentSubmissionStatus getStatus();

        Long getTotal();
    }

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // Học sinh xem lịch sử làm bài của mình trong khoá học
    Page<AssignmentSubmission> findAllByStudentId(Long studentId, Pageable pageable);
}
