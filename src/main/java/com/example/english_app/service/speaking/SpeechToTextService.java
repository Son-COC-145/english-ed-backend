package com.example.english_app.service.speaking;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeechToTextService {

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    public String trancribeAudio(MultipartFile audioFile) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key="
                + geminiApiKey;
        try {
            String base64Audio = Base64.getEncoder().encodeToString(audioFile.getBytes());

            String mimeType = audioFile.getContentType();
            if (mimeType == null || mimeType.equals("application/octet-stream")) {
                mimeType = "audio/mp3";
            }

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text",
                                            "Listen to this audio and exactly transcribe what the user is saying in English. Output only the transcript without any intro or outro, do not translate."),
                                    Map.of("inlineData", Map.of(
                                            "mimeType", mimeType,
                                            "data", base64Audio))))));

            String responseString = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = objectMapper.readTree(responseString);

            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText()
                    .trim();
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi giọng nói thành văn bản bằng Gemini", e);
            throw new RuntimeException("Cannot transcribe audio", e);
        }

    }

}
