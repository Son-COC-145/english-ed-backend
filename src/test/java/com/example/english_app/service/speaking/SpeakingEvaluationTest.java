package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.mapper.SpeakingMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class SpeakingEvaluationTest {
    final ObjectMapper mapper = new ObjectMapper();
    final SpeakingJson json = new SpeakingJson(mapper);

    @Test void mapperPreservesMetricsObjectsArraysAndLegacyFallbacks() {
        var speakingMapper = new SpeakingMapper(mapper);
        var turn = SpeakingTurn.builder().build();
        turn.setAudioMetricsJson("{\"wordCount\":6}");
        assertThat(speakingMapper.toTurnResponse(turn).getAudioMetrics())
                .isEqualTo(Map.of("wordCount", 6));
        turn.setAudioMetricsJson("[1,2]");
        assertThat(speakingMapper.toTurnResponse(turn).getAudioMetrics()).isEqualTo(List.of(1, 2));
        turn.setAudioMetricsJson("null");
        assertThat(speakingMapper.toTurnResponse(turn).getAudioMetrics()).isNull();
        for (String legacy : List.of("malformed JSON", "123", "\"legacy text\"")) {
            turn.setAudioMetricsJson(legacy);
            assertThat(speakingMapper.toTurnResponse(turn).getAudioMetrics()).isEqualTo(legacy);
        }
    }

    @Test void correctNaturalSentenceAcceptsEmptyCorrections() {
        assertThatCode(() -> SpeakingAiClient.validateCorrections(json.read(
                "{\"grammar_errors\":[],\"vocabulary_suggestions\":[]}"), "I like tea."))
                .doesNotThrowAnyException();
    }

    @Test void rejectsHallucinatedCorrectionAndWrongTypes() {
        var result = json.read("""
                {"grammar_errors":[{"original":"coffee","correction":"tea","explanation":"reason"}],"vocabulary_suggestions":[]}
                """);
        assertThatThrownBy(() -> SpeakingAiClient.validateCorrections(result, "I like tea."))
                .isInstanceOf(IllegalArgumentException.class);
        ((ObjectNode) result.path("grammar_errors").get(0)).put("original", "tea").put("correction", 123);
        assertThatThrownBy(() -> SpeakingAiClient.validateCorrections(result, "I like tea."))
                .isInstanceOf(IllegalArgumentException.class);
    }

    ObjectNode report() {
        return (ObjectNode) json.read("""
                {"criteria":[{"goal_index":0,"achieved":true,"turn_id":4,"evidence":"tea","explanation":"Asked for tea"}],
                 "general_feedback":{"strengths":"Clear","weaknesses":"","overall_feedback":"Good"},"task_completion_score":999}
                """);
    }
    List<SpeakingTurn> turns(SpeakerRole role) {
        return List.of(SpeakingTurn.builder().id(4L).speaker(role).transcriptText("I want tea.").build());
    }

    @Test void scoreIsDerivedFromChecklistRatherThanProviderScore() {
        var result = SpeakingAiClient.validateGoals(report(), new String[]{"Order tea"}, turns(SpeakerRole.STUDENT));
        assertThat(result.path("task_completion_score").asInt()).isEqualTo(100);
    }

    @Test void rejectsAiEvidenceWrongTurnAndCoercedIndex() {
        assertThatThrownBy(() -> SpeakingAiClient.validateGoals(report(), new String[]{"Order tea"}, turns(SpeakerRole.AI)))
                .isInstanceOf(IllegalArgumentException.class);
        var result = report();
        ((ObjectNode) result.path("criteria").get(0)).put("turn_id", 5);
        assertThatThrownBy(() -> SpeakingAiClient.validateGoals(result, new String[]{"Order tea"}, turns(SpeakerRole.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class);
        ((ObjectNode) result.path("criteria").get(0)).put("turn_id", 4).put("goal_index", "0");
        assertThatThrownBy(() -> SpeakingAiClient.validateGoals(result, new String[]{"Order tea"}, turns(SpeakerRole.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void rejectsEmptyChecklistAndUnmetGoalWithEvidence() {
        assertThatThrownBy(() -> SpeakingAiClient.validateGoals(report(), new String[0], turns(SpeakerRole.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class);
        var result = report();
        ((ObjectNode) result.path("criteria").get(0)).put("achieved", false);
        assertThatThrownBy(() -> SpeakingAiClient.validateGoals(result, new String[]{"Order tea"}, turns(SpeakerRole.STUDENT)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void legacyCorrectionsMapToStableDtoAndMissingListsAreEmpty() {
        var mapping = new SpeakingMapper(mapper);
        var turn = SpeakingTurn.builder().grammarErrorsJson("[{\"error\":\"I is\",\"correction\":\"I am\",\"explanation\":\"Agreement\"}]").build();
        var response = mapping.toTurnResponse(turn);
        assertThat(response.getGrammarErrors().getFirst().original()).isEqualTo("I is");
        assertThat(response.getVocabularySuggestions()).isEmpty();
    }
}
