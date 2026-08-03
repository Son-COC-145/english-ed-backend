package com.example.english_app.service.integration;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TtsGenerationService {
    @Value("${elevenlabs.api-key}")
    private String apiKey;

    private final CloudinaryService cloudinaryService;
    private final RestClient restClient = RestClient.create();

    // Giọng nam Mỹ (Adam) hoặc thay bằng Voice ID khác
    private final String US_VOICE_ID = "pNInz6obpgDQGcFmaJgB";

    public String generateAudio(String word) {
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + US_VOICE_ID;

        Map<String, Object> requestBody = Map.of(
                "text", word,
                "model_id", "eleven_multilingual_v2",
                "voice_settings", Map.of("stability", 0.5, "similarity_boost", 0.5));

        try {
            byte[] audioBytes = restClient.post()
                    .uri(url)
                    .header("xi-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(byte[].class);

            String publicId = "audio/" + word.replaceAll("\\s+", "_") + "_"
                    + UUID.randomUUID().toString().substring(0, 5);

            return cloudinaryService.uploadFile(audioBytes, "video", publicId);
        } catch (Exception e) {
            log.error("Lỗi khi gọi ElevenLabs", e);
            throw new RuntimeException("ElevenLabs Error");
        }
    }

}
