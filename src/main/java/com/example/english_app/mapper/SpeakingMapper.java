package com.example.english_app.mapper;

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

        Object grammarErrors = parseJson(entity.getGrammarErrorsJson());
        Object vocabularySuggestions = parseJson(entity.getVocabularySuggestionsJson());

        return SpeakingTurnResponse.builder()
                .id(entity.getId())
                .turnIndex(entity.getTurnIndex())
                .speaker(entity.getSpeaker())
                .transcriptText(entity.getTranscriptText())
                .audioUrl(entity.getAudioUrl())
                .grammarErrors(grammarErrors)
                .vocabularySuggestions(vocabularySuggestions)
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
                log.warn("Failed to parse JSON string: {}", jsonString);
                return jsonString; // Fallback to raw string
            }
        }
    }
}
