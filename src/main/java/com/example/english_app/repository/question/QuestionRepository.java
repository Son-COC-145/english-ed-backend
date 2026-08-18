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
     * <p>Dùng LIMIT 1 trực tiếp trong SQL để PostgreSQL không cần load toàn bộ
     * bảng vào memory rồi Java mới lấy get(0). Với bảng lớn, đây là sự khác biệt
     * giữa O(n log n) và O(1) I/O.
     *
     * @param cefrLevel  Trình độ CEFR cần lấy câu hỏi.
     * @param excludeIds Danh sách ID câu hỏi đã trả lời (không lấy lại).
     * @return Optional chứa câu hỏi nếu còn, empty nếu hết câu hỏi ở level này.
     */
    @Query(value = "SELECT q FROM Question q " +
            "WHERE q.cefrLevel = :level " +
            "AND q.isActive = true " +
            "AND q.id NOT IN :excludeIds " +
            "ORDER BY FUNCTION('RANDOM') " +
            "LIMIT 1")
    Optional<Question> findOneRandomByCefrLevelExcluding(
            @Param("level") CefrLevel cefrLevel,
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



