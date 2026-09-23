package com.example.english_app.repository.question;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByCefrLevelAndSkillAndIsActiveTrue(CefrLevel cefrLevel, Skill skill);

    List<Question> findByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);

    /**
     * Lấy ngẫu nhiên MỘT câu hỏi thuộc level chỉ định, loại trừ các câu đã trả lời.
     *
     * <p>Dùng native PostgreSQL query với RANDOM() + covering index
     * {@code idx_questions_active_by_level} để tránh full table scan.
     * Với 15 câu mỗi placement test và bảng questions vài trăm rows,
     * query này chạy trong < 5ms.
     *
     * @param cefrLevel  Trình độ CEFR cần lấy câu hỏi (tên enum dưới dạng String).
     * @param excludeIds Danh sách ID câu hỏi đã trả lời (không lấy lại).
     * @return Optional chứa câu hỏi nếu còn, empty nếu hết câu hỏi ở level này.
     */
    @Query(value = "SELECT * FROM questions " +
            "WHERE cefr_level = :level " +
            "AND is_active = true " +
            "AND id NOT IN :excludeIds " +
            "ORDER BY RANDOM() " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Question> findOneRandomByCefrLevelExcluding(
            @Param("level") String cefrLevel,
            @Param("excludeIds") List<Long> excludeIds);

    /**
     * Lấy ngẫu nhiên MỘT câu hỏi thuộc level và skill chỉ định, loại trừ các câu đã trả lời.
     *
     * @param level Trình độ CEFR cần lấy câu hỏi (tên enum dưới dạng String).
     * @param skill Kỹ năng cần lấy câu hỏi (tên enum dưới dạng String).
     * @param excludeIds Danh sách ID câu hỏi đã trả lời (không lấy lại).
     * @return Optional chứa câu hỏi nếu còn, empty nếu hết câu hỏi ở level và skill này.
     */
    @Query(value = "SELECT * FROM questions " +
            "WHERE cefr_level = :level AND skill = :skill " +
            "AND is_active = true " +
            "AND id NOT IN :excludeIds " +
            "ORDER BY RANDOM() " +
            "LIMIT 1",
            nativeQuery = true)
    Optional<Question> findOneRandomByLevelAndSkillExcluding(
            @Param("level") String level,
            @Param("skill") String skill,
            @Param("excludeIds") List<Long> excludeIds);

    long countByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);

    @Query("SELECT q FROM Question q WHERE " +
           "(:level IS NULL OR q.cefrLevel = :level) AND " +
           "(:skill IS NULL OR q.skill = :skill) AND " +
           "(:isActive IS NULL OR q.isActive = :isActive) " +
           "ORDER BY q.id DESC")
    org.springframework.data.domain.Page<Question> findByFilters(
            @Param("level") CefrLevel level,
            @Param("skill") Skill skill,
            @Param("isActive") Boolean isActive,
            org.springframework.data.domain.Pageable pageable);

    long countByIsActiveTrue();
    long countByIsActiveFalse();

    /** Đếm số câu hỏi active theo từng CEFR level: trả về [cefrLevel, count]. */
    @Query("SELECT q.cefrLevel, COUNT(q) FROM Question q WHERE q.isActive = true GROUP BY q.cefrLevel")
    List<Object[]> countActiveGroupByLevel();

    /** Đếm số câu hỏi active theo từng kỹ năng: trả về [skill, count]. */
    @Query("SELECT q.skill, COUNT(q) FROM Question q WHERE q.isActive = true GROUP BY q.skill")
    List<Object[]> countActiveGroupBySkill();

    /** Breakdown đầy đủ (cefrLevel, skill, isActive, count) cho bảng thống kê chi tiết. */
    @Query("SELECT q.cefrLevel, q.skill, q.isActive, COUNT(q) FROM Question q " +
           "GROUP BY q.cefrLevel, q.skill, q.isActive ORDER BY q.cefrLevel, q.skill")
    List<Object[]> countGroupByLevelAndSkillAndActive();
}



