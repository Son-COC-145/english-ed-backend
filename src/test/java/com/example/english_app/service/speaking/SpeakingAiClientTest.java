package com.example.english_app.service.speaking;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class SpeakingAiClientTest {
    private final ObjectMapper mapper=new ObjectMapper();
    private HttpServer server;
    private SpeakingAiClient client;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        client=new SpeakingAiClient(new SpeakingJson(mapper));
        ReflectionTestUtils.setField(client,"apiKey","test-only");
        ReflectionTestUtils.setField(client,"model","test-model");
        ReflectionTestUtils.setField(client,"baseUrl","http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void close() { server.stop(0); client.close(); }
    private void response(String body) {
        server.createContext("/chat/completions",exchange->{
            exchange.getRequestBody().readAllBytes();
            byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
    }
    @Test void streamsActualProviderDeltas() throws Exception {
        response("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n\ndata: {\"choices\":[{\"delta\":{\"content\":\" there!\"}}]}\n\ndata: [DONE]\n\n");
        var chunks=new ArrayList<String>();
        String text=client.reply(mapper.readTree("{}"),List.of(),chunks::add);
        assertThat(text).isEqualTo("Hello there!");
        assertThat(chunks).contains("Hello","Hello there!");
    }
    @Test void incompleteStreamIsNotAcceptedAsSuccess() {
        response("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n");
        assertThatThrownBy(()->client.reply(mapper.readTree("{}"),List.of(),ignored->{})).isInstanceOf(java.io.IOException.class);
    }
    @Test void acceptsNoCorrectionsForCorrectSentence() throws Exception {
        SpeakingAiClient.validateCorrections(mapper.readTree("{\"grammar_errors\":[],\"vocabulary_suggestions\":[]}"),"I went yesterday.");
    }
    @Test void rejectsInventedOrUnchangedCorrection() throws Exception {
        var invalid=mapper.readTree("{\"grammar_errors\":[{\"original\":\"I go\",\"correction\":\"I went\",\"explanation\":\"Past tense\"}],\"vocabulary_suggestions\":[]}");
        assertThatThrownBy(()->SpeakingAiClient.validateCorrections(invalid,"I went yesterday.")).isInstanceOf(IllegalArgumentException.class);
    }
}
