package com.example.english_app.entity.ipa;

import com.example.english_app.entity.enums.PronunciationRuleCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "ipa_pronunciation_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpaPronunciationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PronunciationRuleCategory category;

    @Column(name = "title_vi", nullable = false, length = 200)
    private String titleVi;

    @Column(name = "summary_vi", nullable = false, length = 500)
    private String summaryVi;

    @Column(name = "content_markdown", nullable = false, columnDefinition = "text")
    private String contentMarkdown;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "examples_json", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String examplesJson = "[]";

    @Column(name = "order_index", nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
