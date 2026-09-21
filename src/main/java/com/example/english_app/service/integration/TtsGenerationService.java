package com.example.english_app.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.exception.AppException;
import com.example.english_app.service.speaking.AudioMetricsService;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.net.http.HttpClient;
import java.time.Duration;
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
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();
    private final HttpClient audioHttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

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
            throw ErrorCode.SPEAKING_TTS_UNAVAILABLE.toException();
        }
    }

    public byte[] generateAudioStream(String text, String customVoiceId) {
        if (text == null || text.trim().isEmpty()) {
            return new byte[0];
        }

        String voiceId = (customVoiceId != null && !customVoiceId.trim().isEmpty()) ? customVoiceId : defaultVoiceId;
        String url = apiUrl + "/" + voiceId + "?optimize_streaming_latency=2&output_format=mp3_44100_128";

        try {
            var request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("xi-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of(
                            "text", text, "model_id", "eleven_multilingual_v2",
                            "voice_settings", Map.of("stability", 0.5, "similarity_boost", 0.75)))))
                    .build();
            var response = audioHttpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length == 0)
                throw ErrorCode.SPEAKING_TTS_INVALID_OUTPUT.toException();
            validateMp3(response.body());
            return response.body();
        } catch (AppException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ErrorCode.SPEAKING_TTS_INTERRUPTED.toException();
        } catch (Exception e) {
            throw ErrorCode.SPEAKING_TTS_UNAVAILABLE.toException();
        }
    }

    private void validateMp3(byte[] audio) {
        try {
            if (!"audio/mpeg".equals(AudioMetricsService.detectMime(audio))) {
                throw ErrorCode.SPEAKING_TTS_INVALID_OUTPUT.toException();
            }
        } catch (AppException invalidAudio) {
            throw ErrorCode.SPEAKING_TTS_INVALID_OUTPUT.toException();
        }
    }
}
