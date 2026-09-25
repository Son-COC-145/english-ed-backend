package com.example.english_app.repository.question;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
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
     * Chọn một câu chưa được trả lời bằng một DB round-trip.
     * Ưu tiên CEFR gần target nhất; nếu cùng khoảng cách thì ưu tiên level thấp hơn,
     * sau đó random giữa các câu cùng hạng.
     */
    @Query(value = """
            SELECT q.*
            FROM questions q
            WHERE q.skill = :skill
              AND q.is_active = true
              AND (:skill <> 'LISTENING' OR q.placement_audio_url IS NOT NULL)
              AND NOT EXISTS (
                  SELECT 1
                  FROM placement_test_answers a
                  WHERE a.session_id = :sessionId
                    AND a.question_id = q.id
              )
            ORDER BY
              ABS((CASE q.cefr_level
                    WHEN 'A1' THEN 0
                    WHEN 'A2' THEN 1
                    WHEN 'B1' THEN 2
                    WHEN 'B2' THEN 3
                    WHEN 'C1' THEN 4
                    WHEN 'C2' THEN 5
                    ELSE 99
                  END) - :targetRank),
              CASE WHEN (CASE q.cefr_level
                    WHEN 'A1' THEN 0
                    WHEN 'A2' THEN 1
                    WHEN 'B1' THEN 2
                    WHEN 'B2' THEN 3
                    WHEN 'C1' THEN 4
                    WHEN 'C2' THEN 5
                    ELSE 99
                  END) < :targetRank THEN 0 ELSE 1 END,
              RANDOM()
            LIMIT 1
            """, nativeQuery = true)
    Optional<Question> findBestAvailableForPlacement(
            @Param("sessionId") Long sessionId,
            @Param("skill") String skill,
            @Param("targetRank") int targetRank);

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

    /** Learner-ready questions; listening items count only after media was pre-generated. */
    @Query("SELECT q.skill, COUNT(q) FROM Question q WHERE q.isActive = true " +
           "AND (q.skill <> com.example.english_app.entity.enums.Skill.LISTENING " +
           "OR q.placementAudioUrl IS NOT NULL) GROUP BY q.skill")
    List<Object[]> countPlacementReadyGroupBySkill();

    List<Question> findBySkillAndQuestionType(Skill skill, QuestionType questionType);

    /** Breakdown đầy đủ (cefrLevel, skill, isActive, count) cho bảng thống kê chi tiết. */
    @Query("SELECT q.cefrLevel, q.skill, q.isActive, COUNT(q) FROM Question q " +
           "GROUP BY q.cefrLevel, q.skill, q.isActive ORDER BY q.cefrLevel, q.skill")
    List<Object[]> countGroupByLevelAndSkillAndActive();
}



