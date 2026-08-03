package com.example.english_app.service.integration;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.english_app.dto.ai.EvaluationResultDto;
import com.example.english_app.dto.response.AiVocabularyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiTextService {
    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.api-url}")
    private String geminiApiUrl;

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    public AiVocabularyResponse generateVocabularyData(String word, String topic, String cefr) {
        String prompt = String.format(
                "Generate JSON for the English word '%s' in topic '%s', CEFR '%s'. " +
                        "JSON must exactly follow this schema and contain ONLY JSON (no markdown tags): " +
                        "{ \"ipaTranscription\": \"...\", \"definitionVi\": \"...\", \"nuanceNote\": \"...\", " +
                        "\"exampleSentencesJson\": [ {\"en\": \"...\", \"vi\": \"...\"} ], " +
                        "\"collocationJson\": [ {\"collocation\": \"...\", \"vi\": \"...\"} ], " +
                        "\"dialogueJson\": [ {\"speaker\": \"A\", \"en\": \"...\", \"vi\": \"...\"} ] }",
                word, topic, cefr);

        String url = geminiApiUrl + "?key=" + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            String jsonString = (String) part.get("text");

            jsonString = jsonString.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(jsonString, AiVocabularyResponse.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini API", e);
            throw new RuntimeException("Gemini AI error");
        }
    }

    public EvaluationResultDto evaluateSpeakingSession(String transcript, String goalDescription) {
        String prompt = String.format(
            "You are an expert English teacher. Evaluate the following conversation transcript. " +
            "The student's goal was: '%s'.\n\n" +
            "Transcript:\n%s\n\n" +
            "You MUST return the evaluation strictly in this JSON format (no markdown formatting):\n" +
            "{\n" +
            "  \"turns_evaluation\": [\n" +
            "    {\n" +
            "      \"turn_index\": 1,\n" +
            "      \"grammar_errors\": [{\"error\": \"...\", \"correction\": \"...\", \"explanation\": \"...\"}],\n" +
            "      \"vocabulary_suggestions\": [{\"word\": \"...\", \"suggestion\": \"...\"}]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"task_completion_score\": 80,\n" +
            "  \"intonation_score\": 75,\n" +
            "  \"general_feedback\": {\n" +
            "    \"strengths\": \"...\",\n" +
            "    \"weaknesses\": \"...\",\n" +
            "    \"overall_feedback\": \"...\"\n" +
            "  }\n" +
            "}", goalDescription, transcript);

        String url = geminiApiUrl + "?key=" + apiKey;
        
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("responseMimeType", "application/json")
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            List<?> candidates = (List<?>) response.get("candidates");
            Map<?, ?> content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            String jsonString = (String) ((Map<?, ?>) ((List<?>) content.get("parts")).get(0)).get("text");

            return objectMapper.readValue(jsonString, EvaluationResultDto.class);
        } catch (Exception e) {
            log.error("Lỗi khi gọi Gemini AI chấm điểm hội thoại", e);
            throw new RuntimeException("Gemini Evaluation error");
        }
    }
}

