package com.example.english_app.entity.speaking;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.vocabulary.Topic;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "speaking_scenarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakingScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "title_vi", nullable = false, length = 200)
    private String titleVi;

    @Column(name = "title_en", nullable = false, length = 200)
    private String titleEn;

    @Column(name = "context_description", nullable = false, columnDefinition = "TEXT")
    private String contextDescription;

    @Column(name = "ai_role_name", nullable = false, length = 100)
    private String aiRoleName;

    @Column(name = "ai_role_avatar_url", length = 500)
    private String aiRoleAvatarUrl;

    @Column(name = "ai_system_prompt", nullable = false, columnDefinition = "TEXT")
    private String aiSystemPrompt;

    @Column(name = "goal_description", nullable = false, columnDefinition = "TEXT")
    private String goalDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hint_phrases_json", columnDefinition = "jsonb")
    private String hintPhrasesJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false)
    private CefrLevel cefrLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
