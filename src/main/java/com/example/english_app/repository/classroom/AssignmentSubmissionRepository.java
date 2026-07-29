package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.AssignmentSubmission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Page<AssignmentSubmission> findAllByAssignmentId(Long assignmentId, Pageable pageable);

    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // Học sinh xem lịch sử làm bài của mình trong khoá học
    Page<AssignmentSubmission> findAllByStudentId(Long studentId, Pageable pageable);
}
