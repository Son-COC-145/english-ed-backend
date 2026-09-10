package com.example.english_app.repository.vocabulary;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearningStatus;
import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.entity.vocabulary.Vocabulary;

@Repository
public interface StudentVocabularyProgressRepository extends JpaRepository<StudentVocabularyProgress, Long> {

    Optional<StudentVocabularyProgress> findByStudentIdAndVocabularyId(Long studentId, Long vocabularyId);

    Optional<StudentVocabularyProgress> findByIdAndStudentId(Long id, Long studentId);

    Page<StudentVocabularyProgress> findByStudentId(Long studentId, Pageable pageable);

    // ─── Daily Mission (internal) ────────────────────────────────────────────
    @Query(value = "SELECT v FROM StudentVocabularyProgress p " +
           "JOIN p.vocabulary v " +
           "LEFT JOIN FETCH v.topic t " +
           "LEFT JOIN FETCH v.createdBy c " +
           "WHERE p.student.id = :studentId " +
           "AND p.nextReviewAt <= :now ",
           countQuery = "SELECT COUNT(p) FROM StudentVocabularyProgress p WHERE p.student.id = :studentId AND p.nextReviewAt <= :now")
    Page<Vocabulary> findVocabulariesToReview(
           @Param("studentId") Long studentId,
           @Param("now") LocalDateTime now,
           Pageable pageable);

    // ─── SRS Due-Review Queue ────────────────────────────────────────────────

    /**
     * Lấy danh sách StudentVocabularyProgress đến hạn ôn tập, có filter tùy chọn.
     * Backend quyết định thứ tự: ưu tiên từ quá hạn lâu nhất (nextReviewAt ASC).
     */
    @Query("SELECT p FROM StudentVocabularyProgress p " +
           "JOIN FETCH p.vocabulary v " +
           "LEFT JOIN FETCH v.topic t " +
           "WHERE p.student.id = :studentId " +
           "AND p.nextReviewAt <= :now " +
           "AND p.status <> com.example.english_app.entity.enums.LearningStatus.NEW " +
           "AND (:topicId IS NULL OR v.topic.id = :topicId) " +
           "AND (:cefrLevel IS NULL OR v.cefrLevel = :cefrLevel) " +
           "ORDER BY p.nextReviewAt ASC")
    Page<StudentVocabularyProgress> findDueReviews(
           @Param("studentId") Long studentId,
           @Param("now") LocalDateTime now,
           @Param("topicId") Short topicId,
           @Param("cefrLevel") CefrLevel cefrLevel,
           Pageable pageable);

    /** Tổng số từ đến hạn (không phân trang) – dùng cho dueCount field */
    @Query("SELECT COUNT(p) FROM StudentVocabularyProgress p " +
           "WHERE p.student.id = :studentId " +
           "AND p.nextReviewAt <= :now " +
           "AND p.status <> com.example.english_app.entity.enums.LearningStatus.NEW")
    long countDueReviews(@Param("studentId") Long studentId, @Param("now") LocalDateTime now);

    // ─── Vocabulary Progress Filters ─────────────────────────────────────────

    /** Filter theo status – dùng cho màn Đang học / Đang ôn / Đã thuộc */
    Page<StudentVocabularyProgress> findByStudentIdAndStatus(
            Long studentId, LearningStatus status, Pageable pageable);

    /** Filter theo status và dueOnly */
    @Query("SELECT p FROM StudentVocabularyProgress p " +
           "WHERE p.student.id = :studentId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:dueOnly = false OR p.nextReviewAt <= :now) " +
           "ORDER BY p.lastPracticedAt DESC")
    Page<StudentVocabularyProgress> findByStudentIdWithFilters(
           @Param("studentId") Long studentId,
           @Param("status") LearningStatus status,
           @Param("dueOnly") boolean dueOnly,
           @Param("now") LocalDateTime now,
           Pageable pageable);

    // ─── Summary Counts ──────────────────────────────────────────────────────

    /** Đếm theo status – dùng cho Vocabulary Summary */
    @Query("SELECT p.status, COUNT(p) FROM StudentVocabularyProgress p " +
           "WHERE p.student.id = :studentId GROUP BY p.status")
    List<Object[]> countByStudentIdGroupByStatus(@Param("studentId") Long studentId);

    /** Tổng số từ đã có progress (bất kỳ status) */
    long countByStudentId(Long studentId);

    // ─── Topic Progress Counts ───────────────────────────────────────────────

    /** Đếm số từ MASTERED theo từng topicId cho danh sách topic */
    @Query("SELECT p.vocabulary.topic.id, COUNT(p) FROM StudentVocabularyProgress p " +
           "WHERE p.student.id = :studentId " +
           "AND p.status = com.example.english_app.entity.enums.LearningStatus.MASTERED " +
           "AND p.vocabulary.topic.id IN :topicIds " +
           "GROUP BY p.vocabulary.topic.id")
    List<Object[]> countMasteredByStudentIdAndTopicIds(
            @Param("studentId") Long studentId,
            @Param("topicIds") List<Short> topicIds);

    /** Đếm số từ MASTERED trong một topic cụ thể */
    @Query("SELECT COUNT(p) FROM StudentVocabularyProgress p " +
           "WHERE p.student.id = :studentId " +
           "AND p.status = com.example.english_app.entity.enums.LearningStatus.MASTERED " +
           "AND p.vocabulary.topic.id = :topicId")
    long countMasteredByStudentIdAndTopicId(
            @Param("studentId") Long studentId,
            @Param("topicId") Short topicId);
}

