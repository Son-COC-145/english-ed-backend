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
            "/speech/recognition/conversation/cognitiveservices/v1?language=en-US&format=detailed";

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
            throw new AppException(ErrorCode.AUDIO_EMPTY_OR_CORRUPT);
        }
        if (audioBytes.length > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.AUDIO_PAYLOAD_TOO_LARGE);
        }
        if (referenceText == null || referenceText.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "referenceText không được để trống");
        }
    }

    // ─── Format detection (magic bytes) ───────────────────────────────────────

    /**
     * Nhận diện format audio từ 4 byte đầu tiên (magic bytes).
     * Bảo vệ khỏi tấn công file giả mạo extension.
     *
     * @throws AppException UNSUPPORTED_AUDIO_FORMAT nếu format không được hỗ trợ.
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
        throw new AppException(ErrorCode.UNSUPPORTED_AUDIO_FORMAT);
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
                    "Granularity",    "Phoneme",
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
            log.debug("Azure raw response for word='{}': {}", word, responseBody);
            if (!"Success".equals(recognitionStatus)) {
                log.warn("Azure recognition status: {} for word='{}'", recognitionStatus, word);
                return buildUnavailableResult(word);
            }

            JsonNode nBest = root.path("NBest").get(0);
            if (nBest == null) {
                return buildUnavailableResult(word);
            }

            JsonNode pronNode = nBest.path("PronunciationAssessment");
            short accuracyScore     = 0;
            short fluencyScore      = 0;
            short completenessScore = 100;
            short pronScore         = 0;

            if (!pronNode.isMissingNode()) {
                accuracyScore     = (short) Math.round(pronNode.path("AccuracyScore").asDouble(0));
                pronScore         = (short) Math.round(pronNode.path("PronScore").asDouble(accuracyScore));
                fluencyScore      = (short) Math.round(pronNode.path("FluencyScore").asDouble(accuracyScore));
                completenessScore = (short) Math.round(pronNode.path("CompletenessScore").asDouble(100.0));
            } else if (nBest.has("AccuracyScore")) {
                accuracyScore     = (short) Math.round(nBest.path("AccuracyScore").asDouble(0));
                pronScore         = (short) Math.round(nBest.path("PronScore").asDouble(accuracyScore));
                fluencyScore      = (short) Math.round(nBest.path("FluencyScore").asDouble(accuracyScore));
                completenessScore = (short) Math.round(nBest.path("CompletenessScore").asDouble(100.0));
            }

            // Fallback từ mảng Words nếu accuracyScore vẫn là 0
            if (accuracyScore == 0 && nBest.has("Words") && nBest.path("Words").size() > 0) {
                JsonNode firstWordNode = nBest.path("Words").get(0).path("PronunciationAssessment");
                if (!firstWordNode.isMissingNode()) {
                    accuracyScore = (short) Math.round(firstWordNode.path("AccuracyScore").asDouble(0));
                    pronScore = accuracyScore;
                    fluencyScore = accuracyScore;
                    completenessScore = 100;
                }
            }

            // Fallback từ Confidence nếu Azure không trả accuracy score
            if (accuracyScore == 0 && nBest.has("Confidence")) {
                double conf = nBest.path("Confidence").asDouble(0);
                if (conf > 0) {
                    accuracyScore = (short) Math.round(conf * 100.0);
                    pronScore = accuracyScore;
                    fluencyScore = accuracyScore;
                    completenessScore = 100;
                }
            }

            log.info("Azure scores for word='{}': accuracy={}, pron={}, fluency={}", word, accuracyScore, pronScore, fluencyScore);

            short overallScore = calculateOverallScore(pronScore, accuracyScore, completenessScore);
            String scoreColor  = classifyColor(overallScore);
            String scoreLevel  = classifyLevel(overallScore);

            return PronunciationScoreResult.builder()
                    .word(word)
                    .overallScore(overallScore)
                    .accuracyScore(accuracyScore)
                    .fluencyScore(fluencyScore)
                    .completenessScore(completenessScore)
                    .scoreColor(scoreColor)
                    .scoreLevel(scoreLevel)
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

    private String classifyLevel(short score) {
        if (score >= COLOR_GREEN_THRESHOLD)  return "EXCELLENT";
        if (score >= COLOR_YELLOW_THRESHOLD) return "GOOD";
        return "NEEDS_PRACTICE";
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
                .scoreLevel("NONE")
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
