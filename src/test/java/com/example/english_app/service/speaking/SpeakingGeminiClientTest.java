package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class SpeakingGeminiClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> request = new AtomicReference<>();
    private final AtomicReference<String> key = new AtomicReference<>();
    private final AtomicReference<String> query = new AtomicReference<>();
    private HttpServer server;
    private SpeakingAiClient client;

    @BeforeEach
    void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        client = new SpeakingAiClient(new SpeakingJson(mapper));
        ReflectionTestUtils.setField(client, "geminiApiKey", "test-gemini");
        ReflectionTestUtils.setField(client, "geminiApiUrl",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/models/test:generateContent");
    }

    @AfterEach
    void close() {
        server.stop(0);
        client.close();
    }

    private void respond(boolean stream, int status, String body) {
        server.createContext("/models/test:" + (stream ? "streamGenerateContent" : "generateContent"), exchange -> {
            request.set(mapper.readTree(exchange.getRequestBody()));
            key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            query.set(exchange.getRequestURI().getQuery());
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    private String result(String text, String finish) throws Exception {
        return mapper.writeValueAsString(Map.of("candidates", List.of(Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))), "finishReason", finish))));
    }

    @Test
    void defaultsToGeminiAndStreamsConversationHistory() throws Exception {
        respond(true, 200, "data: " + result("Hello", "") + "\n\ndata: " + result(" there!", "STOP") + "\n\n");
        SpeakingTurn ai = new SpeakingTurn();
        ai.setSpeaker(SpeakerRole.AI);
        ai.setStatus("COMPLETED");
        ai.setTranscriptText("Welcome");
        SpeakingTurn student = new SpeakingTurn();
        student.setSpeaker(SpeakerRole.STUDENT);
        student.setStatus("COMPLETED");
        student.setTranscriptText("Hi");
        List<String> progress = new ArrayList<>();
        assertThat(client.reply(mapper.readTree("{\"cefr\":\"A2\"}"), List.of(ai, student), progress::add))
                .isEqualTo("Hello there!");
        assertThat(progress).contains("Hello", "Hello there!");
        assertThat(key.get()).isEqualTo("test-gemini");
        assertThat(query.get()).isEqualTo("alt=sse");
        assertThat(request.get().at("/contents/0/role").asText()).isEqualTo("model");
        assertThat(request.get().at("/contents/1/role").asText()).isEqualTo("user");
        assertThat(request.get().at("/systemInstruction/parts/0/text").asText()).contains("A2");
    }

    @Test
    void transcribesInlineAudioWithoutInventingDuration() throws Exception {
        respond(false, 200, result("{\"text\":\"I went yesterday.\"}", "STOP"));
        byte[] audio = { 1, 2, 3 };
        var transcription = client.transcribe(audio, "audio/wav");
        assertThat(transcription.text()).isEqualTo("I went yesterday.");
        assertThat(transcription.durationSeconds()).isNull();
        assertThat(request.get().at("/contents/0/parts/0/inlineData/mimeType").asText()).isEqualTo("audio/wav");
        assertThat(request.get().at("/contents/0/parts/0/inlineData/data").asText()).isEqualTo("AQID");
    }

    @Test
    void evaluatesUsingJsonMode() throws Exception {
        respond(false, 200, result("{\"grammar_errors\":[],\"vocabulary_suggestions\":[]}", "STOP"));
        SpeakingTurn turn = new SpeakingTurn();
        turn.setTranscriptText("Hello there.");
        assertThat(client.evaluateTurn(turn, "A2").path("grammar_errors").isEmpty()).isTrue();
        assertThat(request.get().at("/generationConfig/responseMimeType").asText()).isEqualTo("application/json");
    }

    @Test
    void rejectsTruncatedStream() throws Exception {
        respond(true, 200, "data: " + result("partial", "") + "\n\n");
        assertThatThrownBy(() -> client.reply(mapper.readTree("{}"), List.of(), ignored -> {
        })).isInstanceOf(IOException.class);
    }

    @Test
    void rejectsTokenLimitedResponse() throws Exception {
        respond(false, 200, result("{\"text\":\"partial\"}", "MAX_TOKENS"));
        assertThatThrownBy(() -> client.transcribe(new byte[] { 1 }, "audio/wav")).isInstanceOf(IOException.class);
    }

    @Test
    void rejectsHttpFailure() {
        respond(true, 429, "{}");
        assertThatThrownBy(() -> client.reply(mapper.readTree("{}"), List.of(), ignored -> {
        }))
                .isInstanceOf(IOException.class).hasMessageContaining("429");
    }
}
