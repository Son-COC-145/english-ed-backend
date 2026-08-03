package com.example.english_app.service.speaking;

import com.example.english_app.dto.redis.MessageDto;
import com.example.english_app.dto.response.AiStreamChunkResponse;
import com.example.english_app.service.integration.TtsGenerationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiStreamingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final TtsGenerationService ttsService;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.api-url}")
    private String geminiApiUrl;

    private final RestClient restClient = RestClient.create();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SseEmitter streamResponse(Long sessionId, String voiceId) {
        SseEmitter emitter = new SseEmitter(60000L); // timeout 60s
        
        executor.execute(() -> {
            try {
                // 1. Đọc lịch sử chat
                String redisKey = "speaking:session:" + sessionId;
                String historyJson = (String) redisTemplate.opsForValue().get(redisKey);
                List<MessageDto> history = objectMapper.readValue(historyJson, new TypeReference<>() {});
                
                // 2. Gọi Gemini 
                String aiText = callGemini(history);
                
                // 3. Cập nhật lịch sử vào Redis
                history.add(new MessageDto("model", aiText));
                redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(history));

                // 4. Chia câu và stream Text + TTS Audio
                String[] sentences = aiText.split("(?<=[.!?])\\s+");
                
                for (int i = 0; i < sentences.length; i++) {
                    String sentence = sentences[i];
                    byte[] audioBytes = ttsService.generateAudioStream(sentence, voiceId); 
                    String audioBase64 = Base64.getEncoder().encodeToString(audioBytes);
                    
                    AiStreamChunkResponse chunk = AiStreamChunkResponse.builder()
                            .textChunk(sentence + " ")
                            .audioBase64(audioBase64)
                            .isFinal(i == sentences.length - 1)
                            .build();
                            
                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                }
                
                emitter.complete();
            } catch (Exception e) {
                log.error("Lỗi stream AI response", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    @SuppressWarnings("unchecked")
    private String callGemini(List<MessageDto> history) {
        String url = geminiApiUrl + "?key=" + geminiApiKey;

        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> systemInstruction = null;

        // Ánh xạ MessageDto sang định dạng của Gemini API
        for (MessageDto msg : history) {
            if ("system".equals(msg.getRole())) {
                systemInstruction = Map.of("parts", List.of(Map.of("text", msg.getContent())));
            } else {
                contents.add(Map.of(
                        "role", msg.getRole(),
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", contents);
        if (systemInstruction != null) {
            requestBody.put("systemInstruction", systemInstruction);
        }

        try {
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            // Parse response JSON trả về từ Gemini
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
            
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini sinh câu trả lời", e);
            return "Sorry, I am having trouble connecting to my brain right now.";
        }
    }
}
