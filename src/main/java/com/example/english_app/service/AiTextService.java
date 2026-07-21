package com.example.english_app.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + apiKey;

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
}
