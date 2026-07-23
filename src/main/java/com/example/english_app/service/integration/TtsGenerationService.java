package com.example.english_app.service.integration;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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

    @Value("${elevenlabs.default-voice-id:pNInz6obpgDQGcFmaJgB}")
    private String defaultVoiceId;

    @Value("${elevenlabs.api-url}")
    private String apiUrl;

    public String generateAudio(String word) {
        String url = apiUrl + "/" + defaultVoiceId;

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

    public byte[] generateAudioStream(String text, String customVoiceId) {
        if (text == null || text.trim().isEmpty()) {
            return new byte[0];
        }

        String voiceId = (customVoiceId != null && !customVoiceId.trim().isEmpty()) ? customVoiceId : defaultVoiceId;
        String url = apiUrl + "/" + voiceId + "?optimize_streaming_latency=2";

        try {
            return restClient.post()
                    .uri(url)
                    .header("xi-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text", text,
                            "model_id", "eleven_multilingual_v2",
                            "voice_settings", Map.of(
                                    "stability", 0.5,
                                    "similarity_boost", 0.75)))
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception e) {
            log.error("Lỗi khi stream ElevenLabs TTS", e);
            return new byte[0];
        }
    }

}
