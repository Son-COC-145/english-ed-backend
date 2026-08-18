package com.example.english_app.service.audio;

import com.example.english_app.config.AzureSpeechConfig;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service gọi Azure Text-to-Speech (TTS) REST API để sinh giọng đọc chuẩn bản ngữ từ văn bản hoặc ký hiệu IPA.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureTtsService {

    public static final String VOICE_MALE_US = "en-US-GuyNeural";
    public static final String VOICE_FEMALE_US = "en-US-AriaNeural";

    private final AzureSpeechConfig azureSpeechConfig;
    private HttpClient httpClient;

    @PostConstruct
    void init() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(azureSpeechConfig.timeoutSeconds()))
                .build();
    }

    /**
     * Chuyển đổi một từ hoặc câu tiếng Anh thành file âm thanh MP3.
     *
     * @param text      Từ vựng hoặc câu cần đọc
     * @param voiceName Tên giọng đọc (VD: VOICE_MALE_US hoặc VOICE_FEMALE_US)
     * @return Mảng byte chứa định dạng MP3
     */
    public byte[] synthesizeWord(String text, String voiceName) {
        String ssml = String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                "  <voice name='%s'>" +
                "    <prosody rate='-5%%'>%s</prosody>" +
                "  </voice>" +
                "</speak>",
                voiceName != null ? voiceName : VOICE_MALE_US,
                escapeXml(text)
        );

        return callTtsApi(ssml);
    }

    /**
     * Chuyển đổi âm vị IPA thành âm thanh MP3 chuẩn xác bằng thẻ SSML phoneme.
     *
     * @param ipaSymbol Ký hiệu IPA (VD: "iː", "ʃ", "æ")
     * @param voiceName Tên giọng đọc (VD: VOICE_MALE_US hoặc VOICE_FEMALE_US)
     * @return Mảng byte MP3
     */
    public byte[] synthesizePhoneme(String ipaSymbol, String voiceName) {
        // Dùng thẻ SSML phoneme với bảng mã alphabet="ipa"
        String cleanIpa = ipaSymbol.replace("/", "").trim();
        String ssml = String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                "  <voice name='%s'>" +
                "    <phoneme alphabet='ipa' ph='%s'>%s</phoneme>" +
                "  </voice>" +
                "</speak>",
                voiceName != null ? voiceName : VOICE_MALE_US,
                cleanIpa,
                cleanIpa
        );

        try {
            return callTtsApi(ssml);
        } catch (Exception e) {
            log.warn("Direct IPA synthesis failed for '{}', falling back to raw pronunciation: {}", cleanIpa, e.getMessage());
            return synthesizeWord(cleanIpa, voiceName);
        }
    }

    private byte[] callTtsApi(String ssml) {
        if (azureSpeechConfig.subscriptionKey() == null || azureSpeechConfig.subscriptionKey().isBlank()) {
            throw new IllegalStateException("Azure Speech Subscription Key is missing. Please set AZURE_SPEECH_KEY in your environment.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(azureSpeechConfig.ttsEndpoint()))
                    .timeout(Duration.ofSeconds(azureSpeechConfig.timeoutSeconds() * 2L))
                    .header("Ocp-Apim-Subscription-Key", azureSpeechConfig.subscriptionKey())
                    .header("Content-Type", "application/ssml+xml")
                    .header("X-Microsoft-OutputFormat", "audio-16khz-128kbitrate-mono-mp3")
                    .header("User-Agent", "EnglishAppBackend")
                    .POST(HttpRequest.BodyPublishers.ofString(ssml))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                log.error("Azure TTS API returned HTTP {}: {}", response.statusCode(), new String(response.body()));
                throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
            }

            return response.body();
        } catch (Exception e) {
            log.error("Error synthesizing speech with Azure TTS: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
