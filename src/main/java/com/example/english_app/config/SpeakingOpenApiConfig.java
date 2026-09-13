package com.example.english_app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

/** Protocol metadata is kept outside DTO fields. */
@Configuration
public class SpeakingOpenApiConfig {
    private static final String PREFIX = "/api/v1/speaking-session";

    @Bean
    public OpenApiCustomizer speakingContract() {
        return api -> {
            if (api.getPaths() == null) return;
            var stream = api.getPaths().get(PREFIX + "/{id}/stream-response");
            if (stream != null && stream.getGet() != null) {
                stream.getGet().getResponses().addApiResponse("200", new ApiResponse()
                        .description("Named SSE; JSON snapshots without an envelope. See x-sse-events and operation description.")
                        .content(new Content().addMediaType("text/event-stream", new MediaType()
                                .schema(new StringSchema()).example("event: done\ndata: {\"sessionId\":42,\"turnId\":103}\n\n"))));
                stream.getGet().addExtension("x-sse-events", Map.of(
                        "transcript", Map.of("schema", "#/components/schemas/SpeakingTurnResponse", "mode", "snapshot"),
                        "text", Map.of("schema", "#/components/schemas/SpeakingTurnResponse", "mode", "snapshot"),
                        "text-final", Map.of("schema", "#/components/schemas/SpeakingTurnResponse", "meaning", "AI text finalized before or after TTS"),
                        "audio", Map.of("turnId", "integer", "audioUrl", "string"),
                        "session", Map.of("sessionId", "integer", "status", "string"),
                        "done", Map.of("sessionId", "integer", "turnId", "integer"),
                        "error", Map.of("sessionId", "integer", "turnId", "integer", "errorCode", "string")));
            }
            var audio = api.getPaths().get(PREFIX + "/{id}/turns/{turnId}/audio");
            if (audio != null && audio.getGet() != null) {
                audio.getGet().getResponses().addApiResponse("200", new ApiResponse().description("Raw bytes; Cache-Control: no-store")
                        .content(new Content().addMediaType("audio/mpeg", binary()).addMediaType("audio/wav", binary())));
                audio.getGet().getResponses().addApiResponse("409", new ApiResponse().description("8106: pending; 8107: generation failed"));
                audio.getGet().getResponses().addApiResponse("404", new ApiResponse().description("8103: session/turn not found or not owned; 8108: bytes missing"));
            }
            for (String path : List.of("/start", "/{id}/audio-input", "/{id}/text-input", "/{id}/hints", "/recover")) {
                var item = api.getPaths().get(PREFIX + path);
                if (item == null) continue;
                Operation operation = item.getPost() != null ? item.getPost() : item.getGet();
                if (operation == null) continue;
                if (operation.getParameters() != null) operation.getParameters().stream()
                        .filter(p -> "Idempotency-Key".equals(p.getName())).forEach(p -> p
                                .description("8–100 ASCII letters/digits/_/-. Start: user+key; input: session+key shared by audio/text; hints: session+key. Same payload returns current resource; changed payload conflicts 409/8104. Committed duplicate does not consume quota; no Speaking key expiry cleanup.")
                                .schema(new StringSchema().minLength(8).maxLength(100).pattern("^[A-Za-z0-9_-]{8,100}$")));
            }
            var input = api.getPaths().get(PREFIX + "/{id}/audio-input");
            if (input != null && input.getPost() != null) {
                input.getPost().setDescription("Multipart file: finalized WAV PCM16 LE, mono 16000Hz. Filename/part MIME are not used for detection; use recording.wav/audio/wav. 1–180 seconds AND <=5MiB (size is stricter for long recordings). Energy gate RMS >=0.012 in a 40ms window. Errors: 400/5012 corrupt, 413/5011 size, 415/8109 format, 400/8110 short, 400/8111 long, 400/8112 silence. Accepted 202 is not STT complete. Blank provider STT fails the turn without automatic retry.");
            }
            enumField(api, "SpeakingTurnResponse", "status", List.of("PENDING", "COMPLETED", "FAILED"));
            enumField(api, "AudioInputResponse", "status", List.of("PENDING", "COMPLETED", "FAILED"));
            enumField(api, "SpeakingTurnResponse", "evaluationStatus", List.of("PENDING", "COMPLETED", "FAILED", "NOT_APPLICABLE"));
            enumField(api, "SpeakingTurnResponse", "audioStatus", List.of("PENDING", "READY", "FAILED", "NOT_APPLICABLE"));
            enumField(api, "SpeakingTurnResponse", "audioAnalysisStatus", List.of("PENDING", "INSUFFICIENT_DATA", "MEASURED", "UNSUPPORTED_OR_CORRUPT_AUDIO", "NO_AUDIO", "NO_SPEECH", "FAILED"));
            for (String schema : List.of("SpeakingSessionResponse", "SessionEvaluationResponse")) {
                enumField(api, schema, "status", List.of("ONGOING", "EVALUATING", "EVALUATION_FAILED", "COMPLETED"));
            }
        };
    }

    private MediaType binary() {
        return new MediaType().schema(new StringSchema().format("binary"));
    }

    private void enumField(OpenAPI api, String type, String field, List<String> values) {
        if (api.getComponents() == null || api.getComponents().getSchemas() == null) return;
        Schema<?> schema = api.getComponents().getSchemas().get(type);
        if (schema == null || schema.getProperties() == null || !schema.getProperties().containsKey(field)) return;
        schema.addProperty(field, new StringSchema()._enum(values));
    }
}
