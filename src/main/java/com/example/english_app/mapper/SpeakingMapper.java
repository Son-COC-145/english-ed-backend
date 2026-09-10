package com.example.english_app.mapper;

import com.example.english_app.dto.response.GrammarCorrection;
import com.example.english_app.dto.response.VocabularySuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.StreamSupport;

import com.example.english_app.dto.response.SpeakingScenarioResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.dto.response.SpeakingTurnResponse;
import com.example.english_app.entity.speaking.SpeakingScenario;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpeakingMapper {

    private final ObjectMapper objectMapper;

    public SpeakingScenarioResponse toScenarioResponse(SpeakingScenario entity) {
        if (entity == null) return null;

        Object hintPhrases = parseJson(entity.getHintPhrasesJson());

        return SpeakingScenarioResponse.builder()
                .id(entity.getId())
                .titleVi(entity.getTitleVi())
                .titleEn(entity.getTitleEn())
                .contextDescription(entity.getContextDescription())
                .aiRoleName(entity.getAiRoleName())
                .aiRoleAvatarUrl(entity.getAiRoleAvatarUrl())
                .aiSystemPrompt(entity.getAiSystemPrompt())
                .goalDescription(entity.getGoalDescription())
                .hintPhrases(hintPhrases)
                .cefrLevel(entity.getCefrLevel())
                .topicId(entity.getTopic() != null ? entity.getTopic().getId() : null)
                .isActive(entity.getIsActive())
                .build();
    }

    public SpeakingSessionResponse toSessionResponse(SpeakingSession entity, List<SpeakingTurn> turns) {
        if (entity == null) return null;

        Object evaluation = parseJson(entity.getEvaluationJson());
        
        List<SpeakingTurnResponse> turnResponses = null;
        if (turns != null) {
            turnResponses = turns.stream()
                    .map(this::toTurnResponse)
                    .collect(Collectors.toList());
        }

        return SpeakingSessionResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .studentId(entity.getStudent() != null ? entity.getStudent().getId() : null)
                .scenarioId(entity.getScenario() != null ? entity.getScenario().getId() : null)
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .hintUsedCount(entity.getHintUsedCount())
                .taskCompletionScore(entity.getTaskCompletionScore())
                .fluencyScore(entity.getFluencyScore())
                .intonationScore(entity.getIntonationScore())
                .xpEarned(entity.getXpEarned())
                .evaluation(evaluation)
                .turns(turnResponses)
                .build();
    }

    public SpeakingTurnResponse toTurnResponse(SpeakingTurn entity) {
        if (entity == null) return null;

        var grammarErrors = corrections(entity.getGrammarErrorsJson(), true);
        var vocabularySuggestions = corrections(entity.getVocabularySuggestionsJson(), false);

        return SpeakingTurnResponse.builder()
                .id(entity.getId())
                .turnIndex(entity.getTurnIndex())
                .status(entity.getStatus())
                .evaluationStatus(entity.getEvaluationStatus())
                .errorCode(entity.getErrorCode())
                .audioMetrics(parseJson(entity.getAudioMetricsJson()))
                .speaker(entity.getSpeaker())
                .transcriptText(entity.getTranscriptText())
                .audioUrl(entity.getAudioUrl())
                .grammarErrors(grammarErrors.stream().map(n -> new GrammarCorrection(
                        n.path("original").asText(n.path("error").asText("")), n.path("correction").asText(""), n.path("explanation").asText(""))).toList())
                .vocabularySuggestions(vocabularySuggestions.stream().map(n -> new VocabularySuggestion(
                        n.path("original").asText(n.path("word").asText("")), n.path("suggestion").asText(""), n.path("reason").asText(""))).toList())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private Object parseJson(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            try {
                return objectMapper.readValue(jsonString, new TypeReference<List<Object>>() {});
            } catch (Exception ex) {
                log.warn("Failed to parse legacy speaking JSON");
                return jsonString; // Fallback to raw string
            }
        }
    }

    private List<JsonNode> corrections(String value, boolean grammar) {
        if (value == null || value.isBlank()) return List.of();
        try {
            var node = objectMapper.readTree(value);
            if (!node.isArray()) throw new IllegalArgumentException("Expected correction array");
            return StreamSupport.stream(node.spliterator(), false).toList();
        } catch (Exception e) {
            log.warn("Cannot parse legacy {} corrections", grammar ? "grammar" : "vocabulary");
            return List.of();
        }
    }
}
