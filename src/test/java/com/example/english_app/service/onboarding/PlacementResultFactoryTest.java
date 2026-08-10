package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.question.Question;
import com.example.english_app.service.onboarding.PlacementResultFactory.SkillScores;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho PlacementResultFactory.
 *
 * <p>PlacementResultFactory là pure function — không có Spring context, không có DB.
 * Tất cả tests đều chạy nhanh (< 1ms mỗi test) mà không cần @SpringBootTest.
 */
@DisplayName("PlacementResultFactory")
class PlacementResultFactoryTest {

    private PlacementResultFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PlacementResultFactory(new ObjectMapper());
    }

    // ─── calculateSkillScore ─────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateSkillScore()")
    class CalculateSkillScore {

        @Test
        @DisplayName("null list → trả về 0")
        void nullList_returns0() {
            assertThat(factory.calculateSkillScore(null)).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("empty list → trả về 0")
        void emptyList_returns0() {
            assertThat(factory.calculateSkillScore(List.of())).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("100% đúng → trả về 100")
        void allCorrect_returns100() {
            List<PlacementTestAnswer> answers = List.of(
                    answer(true), answer(true), answer(true)
            );
            assertThat(factory.calculateSkillScore(answers)).isEqualTo((short) 100);
        }

        @Test
        @DisplayName("0% đúng → trả về 0")
        void allWrong_returns0() {
            List<PlacementTestAnswer> answers = List.of(
                    answer(false), answer(false)
            );
            assertThat(factory.calculateSkillScore(answers)).isEqualTo((short) 0);
        }

        @Test
        @DisplayName("2/3 đúng → trả về 67 (làm tròn)")
        void twoThirdsCorrect_returns67() {
            List<PlacementTestAnswer> answers = List.of(
                    answer(true), answer(true), answer(false)
            );
            assertThat(factory.calculateSkillScore(answers)).isEqualTo((short) 67);
        }

        @Test
        @DisplayName("1/2 đúng → trả về 50 (chính xác)")
        void halfCorrect_returns50() {
            List<PlacementTestAnswer> answers = List.of(
                    answer(true), answer(false)
            );
            assertThat(factory.calculateSkillScore(answers)).isEqualTo((short) 50);
        }
    }

    // ─── calculateAllSkills ──────────────────────────────────────────────────

    @Nested
    @DisplayName("calculateAllSkills()")
    class CalculateAllSkills {

        @Test
        @DisplayName("group đúng theo Skill — vocab/grammar/reading/listening/pronunciation")
        void groupsBySkillCorrectly() {
            List<PlacementTestAnswer> answers = List.of(
                    answerWithSkill(true,  Skill.VOCABULARY),
                    answerWithSkill(false, Skill.VOCABULARY),   // vocab = 50
                    answerWithSkill(true,  Skill.GRAMMAR),       // grammar = 100
                    answerWithSkill(true,  Skill.READING),       // reading = 100
                    answerWithSkill(false, Skill.LISTENING),     // listening = 0
                    answerWithSkill(true,  Skill.PRONUNCIATION)  // pronunciation = 100
            );

            SkillScores scores = factory.calculateAllSkills(answers);

            assertThat(scores.getVocab()).isEqualTo((short) 50);
            assertThat(scores.getGrammar()).isEqualTo((short) 100);
            assertThat(scores.getReading()).isEqualTo((short) 100);
            assertThat(scores.getListening()).isEqualTo((short) 0);
            assertThat(scores.getPronunciation()).isEqualTo((short) 100);
        }

        @Test
        @DisplayName("kỹ năng không có câu hỏi nào → điểm = 0")
        void missingSkill_returns0() {
            // Chỉ có VOCABULARY, không có GRAMMAR
            List<PlacementTestAnswer> answers = List.of(
                    answerWithSkill(true, Skill.VOCABULARY)
            );

            SkillScores scores = factory.calculateAllSkills(answers);

            assertThat(scores.getGrammar()).isEqualTo((short) 0);
            assertThat(scores.getReading()).isEqualTo((short) 0);
            assertThat(scores.getListening()).isEqualTo((short) 0);
            assertThat(scores.getPronunciation()).isEqualTo((short) 0);
        }
    }

    // ─── buildResponse ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildResponse()")
    class BuildResponse {

        @Test
        @DisplayName("map đúng totalQuestions và correctAnswers")
        void mapsCountsCorrectly() {
            List<PlacementTestAnswer> answers = List.of(
                    answer(true), answer(true), answer(false)
            );
            SkillScores scores = SkillScores.builder()
                    .vocab((short) 80).grammar((short) 60)
                    .reading((short) 70).listening((short) 50).pronunciation((short) 90)
                    .build();

            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.B1, scores, answers, null, false);

            assertThat(resp.getTotalQuestions()).isEqualTo(3);
            assertThat(resp.getCorrectAnswers()).isEqualTo(2);
        }

        @Test
        @DisplayName("cefrLevel khớp với enum name")
        void cefrLevelMapped() {
            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.A2, emptyScores(), List.of(), null, false);

            assertThat(resp.getCefrLevel()).isEqualTo("A2");
        }

        @Test
        @DisplayName("roadmapGenerated = false khi roadmapJson = null")
        void roadmapGeneratedFalse_whenNull() {
            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.B1, emptyScores(), List.of(), null, false);

            assertThat(resp.isRoadmapGenerated()).isFalse();
            assertThat(resp.getSuggestedModules()).isNotEmpty(); // fallback modules
        }

        @Test
        @DisplayName("strengths chứa tối đa 3 kỹ năng cao nhất")
        void strengthsTopThree() {
            SkillScores scores = SkillScores.builder()
                    .vocab((short) 90).grammar((short) 80).reading((short) 70)
                    .listening((short) 40).pronunciation((short) 20)
                    .build();

            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.B2, scores, List.of(), null, false);

            assertThat(resp.getStrengths()).hasSize(3);
            assertThat(resp.getStrengths().get(0)).isEqualTo("Từ vựng"); // highest
        }

        @Test
        @DisplayName("weaknesses chứa tối đa 3 kỹ năng thấp nhất")
        void weaknessesBottomThree() {
            SkillScores scores = SkillScores.builder()
                    .vocab((short) 90).grammar((short) 80).reading((short) 70)
                    .listening((short) 40).pronunciation((short) 20)
                    .build();

            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.B2, scores, List.of(), null, false);

            assertThat(resp.getWeaknesses()).hasSize(3);
            assertThat(resp.getWeaknesses().get(0)).isEqualTo("Phát âm"); // lowest
        }

        @Test
        @DisplayName("message không null với mọi CefrLevel")
        void messageNotNullForAllLevels() {
            for (CefrLevel level : CefrLevel.values()) {
                PlacementResultResponse resp = factory.buildResponse(
                        level, emptyScores(), List.of(), null, false);
                assertThat(resp.getMessage())
                        .as("message for level %s", level)
                        .isNotBlank();
                assertThat(resp.getCefrDescription())
                        .as("description for level %s", level)
                        .isNotBlank();
            }
        }

        @Test
        @DisplayName("suggestedModules fallback đúng theo level A1/A2")
        void suggestedModules_A1Fallback() {
            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.A1, emptyScores(), List.of(), null, false);

            assertThat(resp.getSuggestedModules())
                    .containsExactly("Từ vựng cơ bản", "Phát âm IPA nền", "Speaking cơ bản");
        }

        @Test
        @DisplayName("radarChartData chứa đủ 5 key tiếng Việt")
        void radarChartHasFiveVietnameseKeys() {
            PlacementResultResponse resp = factory.buildResponse(
                    CefrLevel.B1, emptyScores(), List.of(), null, false);

            Map<String, Short> radar = resp.getRadarChartData();
            assertThat(radar).containsKeys("Từ vựng", "Ngữ pháp", "Đọc hiểu", "Nghe", "Phát âm");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private PlacementTestAnswer answer(boolean isCorrect) {
        PlacementTestAnswer a = mock(PlacementTestAnswer.class);
        when(a.getIsCorrect()).thenReturn(isCorrect);
        return a;
    }

    private PlacementTestAnswer answerWithSkill(boolean isCorrect, Skill skill) {
        PlacementTestAnswer a = mock(PlacementTestAnswer.class);
        when(a.getIsCorrect()).thenReturn(isCorrect);
        Question q = mock(Question.class);
        when(q.getSkill()).thenReturn(skill);
        when(a.getQuestion()).thenReturn(q);
        return a;
    }

    private SkillScores emptyScores() {
        return SkillScores.builder()
                .vocab((short) 0).grammar((short) 0)
                .reading((short) 0).listening((short) 0).pronunciation((short) 0)
                .build();
    }
}
