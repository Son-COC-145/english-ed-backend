package com.example.english_app.repository.user;

import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:keyword IS NULL OR " +
            "CAST(u.id AS string) LIKE CONCAT('%', CAST(:keyword AS String), '%') OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')))")
    Page<User> searchUsers(@Param("keyword") String keyword, @Param("role") Role role, Pageable pageable);

    /** Active students not yet enrolled in the course; a null keyword returns all of them. */
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.isActive = true AND " +
            "(:keyword IS NULL OR " +
            "CAST(u.id AS string) LIKE CONCAT('%', CAST(:keyword AS String), '%') OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS String), '%'))) AND " +
            "NOT EXISTS (SELECT 1 FROM CourseStudent cs WHERE cs.course.id = :courseId AND cs.student.id = u.id)")
    Page<User> searchEnrollmentCandidates(@Param("keyword") String keyword, @Param("role") Role role,
            @Param("courseId") Long courseId, Pageable pageable);
}
