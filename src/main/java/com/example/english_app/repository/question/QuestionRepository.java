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
}


