package com.example.english_app.service.speaking;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.dto.redis.MessageDto;
import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.entity.speaking.SpeakingScenario;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeakingSessionService {
    private final SpeakingSessionRepository sessionRepository;
    private final SpeakingScenarioRepository scenarioRepository;
    private final UserRepository userRepository;
    private final SpeakingMapper speakingMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final SpeechToTextService speechToTextService;

    public SpeakingSessionResponse startSession(StartSessionRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        SpeakingScenario scenario = scenarioRepository.findById(request.getScenarioId())
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());

        SpeakingSession session = SpeakingSession.builder()
                .student(user)
                .scenario(scenario)
                .build();

        session = sessionRepository.save(session);

        List<MessageDto> chatHisory = new ArrayList<>();
        chatHisory.add(new MessageDto("system", scenario.getAiSystemPrompt()));

        try {
            String historyJson = objectMapper.writeValueAsString(chatHisory);
            String redisKey = "speaking:session:" + session.getId();

            redisTemplate.opsForValue().set(redisKey, historyJson, Duration.ofHours(2));
        } catch (Exception e) {
            log.error("Lỗi khi lưu lịch sử chat vả Redis", e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }

        return speakingMapper.toSessionResponse(session, null);

    }

    public AudioInputResponse processUserAudio(Long sessionId, MultipartFile audioFile) {
        SpeakingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ErrorCode.SESSION_NOT_FOUND.toException());

        String redisKey = "speaking:session:" + sessionId;
        String historyJson = (String) redisTemplate.opsForValue().get(redisKey);

        if (historyJson == null) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }

        try {
            String trancript = speechToTextService.trancribeAudio(audioFile);

            List<MessageDto> history = objectMapper.readValue(historyJson, new TypeReference<List<MessageDto>>() {
            });

            history.add(new MessageDto("user", trancript));

            String updateHistoryJson = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(redisKey, updateHistoryJson, Duration.ofHours(2));

            return new AudioInputResponse(trancript);
        } catch (Exception e) {
            log.error("Lỗi xử lý audio session {}", sessionId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }
}
