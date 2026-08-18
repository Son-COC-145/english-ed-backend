package com.example.english_app.repository.ipa;

import com.example.english_app.entity.enums.PronunciationRuleCategory;
import com.example.english_app.entity.ipa.IpaPronunciationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IpaPronunciationRuleRepository extends JpaRepository<IpaPronunciationRule, Long> {

    @Query("SELECT r FROM IpaPronunciationRule r WHERE (:category IS NULL OR r.category = :category) AND r.isActive = true ORDER BY r.orderIndex ASC, r.id ASC")
    List<IpaPronunciationRule> findByCategory(@Param("category") PronunciationRuleCategory category);
}
