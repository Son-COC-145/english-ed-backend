package com.example.english_app.service.audio;

import com.example.english_app.config.AzureSpeechConfig;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

/**
 * Implementation của {@link AudioAssessmentPort} sử dụng
 * Azure Cognitive Services Pronunciation Assessment REST API.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Validate magic bytes (WAV / WebM / OGG) → throw nếu format không hợp lệ.
 *   <li>Xác định Content-Type dựa vào format đã phát hiện.
 *   <li>Xây dựng Pronunciation-Assessment header (base64 JSON).
 *   <li>Gọi Azure REST API với timeout cứng = {@code azure.speech.timeout-seconds}.
 *   <li>Parse kết quả → tính overallScore → phân loại màu.
 *   <li>Nếu timeout hoặc lỗi Azure → trả về {@code UNAVAILABLE} (không throw).
 * </ol>
 *
 * <p><b>Thread-safety:</b> {@code HttpClient} là thread-safe, có thể tái sử dụng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureAudioAnalysisService implements AudioAssessmentPort {

    // ─── Magic bytes ──────────────────────────────────────────────────────────

    private static final byte[] WAV_MAGIC  = {0x52, 0x49, 0x46, 0x46};        // "RIFF"
    private static final byte[] WEBM_MAGIC = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3};
    private static final byte[] OGG_MAGIC  = {0x4F, 0x67, 0x67, 0x53};        // "OggS"

    // ─── Azure API constants ──────────────────────────────────────────────────

    private static final String SUBSCRIPTION_KEY_HEADER   = "Ocp-Apim-Subscription-Key";
    private static final String PRONUNCIATION_ASSESS_HEADER = "Pronunciation-Assessment";
    private static final String STT_PATH =
            "/speech/recognition/conversation/cognitiveservices/v1?language=en-US";

    // ─── Score thresholds ─────────────────────────────────────────────────────

    private static final int COLOR_GREEN_THRESHOLD  = 80;
    private static final int COLOR_YELLOW_THRESHOLD = 60;

    // Trọng số cho overallScore: pronunciation = 50%, accuracy = 30%, completeness = 20%
    private static final double WEIGHT_PRON         = 0.50;
    private static final double WEIGHT_ACCURACY     = 0.30;
    private static final double WEIGHT_COMPLETENESS = 0.20;

    // ─── Dependencies ─────────────────────────────────────────────────────────

    private final AzureSpeechConfig azureSpeechConfig;
    private final ObjectMapper objectMapper;

    private HttpClient httpClient;

    @PostConstruct
    void initHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(azureSpeechConfig.timeoutSeconds()))
                .build();
    }

    // ─── Public API (AudioAssessmentPort) ────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Luôn trả về kết quả — không bao giờ throw exception ra ngoài.
     * Mọi lỗi (timeout, Azure error, parse error) đều được catch và trả về
     * {@code status = "UNAVAILABLE"}.
     */
    @Override
    public PronunciationScoreResult assess(byte[] audioBytes, String referenceText) {
        validateNotEmpty(audioBytes, referenceText);

        AudioFormat format = detectFormat(audioBytes);
        log.debug("Pronunciation assessment: word='{}', format={}, size={}B",
                referenceText, format, audioBytes.length);

        try {
            String responseBody = callAzureApi(audioBytes, referenceText, format);
            return parseResponse(responseBody, referenceText);
        } catch (Exception ex) {
            log.warn("Azure pronunciation assessment failed for word='{}': {}",
                    referenceText, ex.getMessage());
            return buildUnavailableResult(referenceText);
        }
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    private void validateNotEmpty(byte[] audioBytes, String referenceText) {
        if (audioBytes == null || audioBytes.length < 4) {
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
        if (referenceText == null || referenceText.isBlank()) {
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
    }

    // ─── Format detection (magic bytes) ───────────────────────────────────────

    /**
     * Nhận diện format audio từ 4 byte đầu tiên (magic bytes).
     * Bảo vệ khỏi tấn công file giả mạo extension.
     *
     * @throws AppException AUDIO_PROCESSING_FAILED nếu format không được hỗ trợ.
     */
    private AudioFormat detectFormat(byte[] audioBytes) {
        byte[] header = Arrays.copyOf(audioBytes, 4);

        if (Arrays.equals(header, WAV_MAGIC)) {
            return AudioFormat.WAV;
        }
        if (Arrays.equals(header, WEBM_MAGIC)) {
            return AudioFormat.WEBM;
        }
        if (Arrays.equals(header, OGG_MAGIC)) {
            return AudioFormat.OGG;
        }

        log.warn("Unsupported audio format. Magic bytes: {}",
                String.format("%02X %02X %02X %02X",
                        header[0], header[1], header[2], header[3]));
        throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
    }

    // ─── Azure API call ───────────────────────────────────────────────────────

    /**
     * Gọi Azure Pronunciation Assessment REST API.
     *
     * <p>API docs:
     * https://learn.microsoft.com/azure/ai-services/speech-service/rest-speech-to-text
     *
     * @throws Exception nếu HTTP call thất bại hoặc timeout — được xử lý bởi caller.
     */
    private String callAzureApi(byte[] audioBytes, String referenceText, AudioFormat format)
            throws Exception {

        String endpoint = azureSpeechConfig.sttEndpoint() + STT_PATH;
        String assessHeader = buildPronunciationAssessHeader(referenceText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(azureSpeechConfig.timeoutSeconds()))
                .header(SUBSCRIPTION_KEY_HEADER, azureSpeechConfig.subscriptionKey())
                .header(PRONUNCIATION_ASSESS_HEADER, assessHeader)
                .header(HttpHeaders.CONTENT_TYPE, format.contentType())
                .POST(HttpRequest.BodyPublishers.ofByteArray(audioBytes))
                .build();

        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            log.warn("Azure API returned HTTP {}: {}", response.statusCode(), response.body());
            throw new RestClientException("Azure Speech API error: HTTP " + response.statusCode());
        }

        return response.body();
    }

    /**
     * Xây dựng giá trị của header {@code Pronunciation-Assessment}.
     *
     * <p>Cấu trúc: JSON config → UTF-8 bytes → Base64 encode.
     */
    private String buildPronunciationAssessHeader(String referenceText) {
        try {
            Map<String, Object> config = Map.of(
                    "ReferenceText",  referenceText,
                    "GradingSystem",  "HundredMark",
                    "Granularity",    "FullText",
                    "EnableMiscue",   false
            );
            String json = objectMapper.writeValueAsString(config);
            return Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AppException(ErrorCode.AUDIO_PROCESSING_FAILED);
        }
    }

    // ─── Response parsing ─────────────────────────────────────────────────────

    /**
     * Parse JSON response của Azure Pronunciation Assessment.
     *
     * <p>Response structure:
     * <pre>
     * {
     *   "RecognitionStatus": "Success",
     *   "NBest": [{
     *     "PronunciationAssessment": {
     *       "AccuracyScore":     95.0,
     *       "FluencyScore":      90.0,
     *       "CompletenessScore": 100.0,
     *       "PronScore":         92.0
     *     }
     *   }]
     * }
     * </pre>
     */
    private PronunciationScoreResult parseResponse(String responseBody, String word) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            String recognitionStatus = root.path("RecognitionStatus").asText();
            if (!"Success".equals(recognitionStatus)) {
                log.warn("Azure recognition status: {} for word='{}'", recognitionStatus, word);
                return buildUnavailableResult(word);
            }

            JsonNode assessment = root
                    .path("NBest").get(0)
                    .path("PronunciationAssessment");

            short accuracyScore     = (short) assessment.path("AccuracyScore").asInt(0);
            short fluencyScore      = (short) assessment.path("FluencyScore").asInt(0);
            short completenessScore = (short) assessment.path("CompletenessScore").asInt(0);
            short pronScore         = (short) assessment.path("PronScore").asInt(0);

            short overallScore = calculateOverallScore(pronScore, accuracyScore, completenessScore);
            String scoreColor  = classifyColor(overallScore);

            return PronunciationScoreResult.builder()
                    .word(word)
                    .overallScore(overallScore)
                    .accuracyScore(accuracyScore)
                    .fluencyScore(fluencyScore)
                    .completenessScore(completenessScore)
                    .scoreColor(scoreColor)
                    .status("SCORED")
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Azure response for word='{}': {}", word, e.getMessage());
            return buildUnavailableResult(word);
        }
    }

    // ─── Score helpers ────────────────────────────────────────────────────────

    /**
     * Tính điểm tổng hợp theo trọng số:
     * 50% PronScore + 30% AccuracyScore + 20% CompletenessScore
     */
    private short calculateOverallScore(short pronScore, short accuracyScore, short completenessScore) {
        double overall = (pronScore * WEIGHT_PRON)
                       + (accuracyScore * WEIGHT_ACCURACY)
                       + (completenessScore * WEIGHT_COMPLETENESS);
        return (short) Math.round(overall);
    }

    /**
     * Phân loại màu hiển thị dựa trên điểm tổng hợp.
     * GREEN ≥ 80, YELLOW 60-79, RED < 60.
     */
    private String classifyColor(short score) {
        if (score >= COLOR_GREEN_THRESHOLD)  return "GREEN";
        if (score >= COLOR_YELLOW_THRESHOLD) return "YELLOW";
        return "RED";
    }

    /** Kết quả fallback khi Azure không khả dụng hoặc timeout. */
    private PronunciationScoreResult buildUnavailableResult(String word) {
        return PronunciationScoreResult.builder()
                .word(word)
                .overallScore(null)
                .accuracyScore(null)
                .fluencyScore(null)
                .completenessScore(null)
                .scoreColor("NONE")
                .status("UNAVAILABLE")
                .build();
    }

    // ─── Inner enum ───────────────────────────────────────────────────────────

    /** Các format audio được hỗ trợ, map với Content-Type tương ứng cho Azure. */
    private enum AudioFormat {
        WAV  ("audio/wav; codecs=audio/pcm; samplerate=16000"),
        WEBM ("audio/webm; codecs=opus"),
        OGG  ("audio/ogg; codecs=opus");

        private final String contentType;

        AudioFormat(String contentType) {
            this.contentType = contentType;
        }

        public String contentType() {
            return contentType;
        }
    }
}
