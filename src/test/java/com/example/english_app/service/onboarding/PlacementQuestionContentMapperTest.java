package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementQuestionContentMapperTest {

    private final PlacementQuestionContentMapper mapper =
            new PlacementQuestionContentMapper(new ObjectMapper());

    @Test
    void listeningResponseNeverLeaksTranscriptOrInternalAudioFields() {
        Question question = base(QuestionType.LISTENING, Skill.LISTENING,
                """
                {"transcript":"the secret answer", "question":"What did you hear?",
                 "options":["A","B"], "audioUrl":"internal", "correctAnswer":"A"}
                """);
        question.setPlacementAudioUrl("https://cdn.example/opaque/4f9d.mp3");

        Map<String, Object> content = mapper.toLearnerContent(question);

        assertThat(content).containsOnlyKeys("audioUrl", "question", "options");
        assertThat(content.get("audioUrl")).isEqualTo("https://cdn.example/opaque/4f9d.mp3");
        assertThat(content.toString()).doesNotContain("secret", "correctAnswer", "internal");
    }

    @Test
    void pronunciationUsesCanonicalServerOwnedContract() {
        Question question = base(QuestionType.PRONUNCIATION, Skill.PRONUNCIATION,
                """
                {"word":"entrepreneur", "ipa":"/ˌɒn.trə.prəˈnɜːr/",
                 "transcript":"must-not-leak"}
                """);

        Map<String, Object> content = mapper.toLearnerContent(question);

        assertThat(content).containsKeys("word", "ipaTranscription", "instruction");
        assertThat(content).doesNotContainKeys("ipa", "transcript", "correctAnswer");
    }

    private Question base(QuestionType type, Skill skill, String content) {
        return Question.builder()
                .id(1L)
                .cefrLevel(CefrLevel.A2)
                .questionType(type)
                .skill(skill)
                .contentJson(content)
                .correctAnswer("A")
                .timeoutSeconds(30)
                .build();
    }
}
