package com.example.english_app.repository.question;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByCefrLevelAndSkillAndIsActiveTrue(CefrLevel cefrLevel, Skill skill);

    List<Question> findByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);

    @Query("SELECT q FROM Question q WHERE q.cefrLevel = :level AND q.isActive = true AND q.id NOT IN :excludeIds ORDER BY FUNCTION('RANDOM')")
    List<Question> findRandomByCefrLevelExcluding(
            @Param("level") CefrLevel cefrLevel,
            @Param("excludeIds") List<Long> excludeIds);

    long countByCefrLevelAndIsActiveTrue(CefrLevel cefrLevel);
}
