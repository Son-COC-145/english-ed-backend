package com.example.english_app.service.speaking;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.dto.redis.MessageDto;
import com.example.english_app.dto.request.EndSessionRequest;
import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.dto.ai.EvaluationResultDto;
import com.example.english_app.entity.speaking.SpeakingScenario;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.speaking.SpeakingTurnRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.integration.AiTextService;
import com.example.english_app.service.integration.CloudinaryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;

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
    private final SpeakingTurnRepository speakingTurnRepository;
    private final AiTextService aiTextService;
    private final CloudinaryService cloudinaryService;

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
            
            // Upload audio file to Cloudinary
            String audioUrl = null;
            try {
                audioUrl = cloudinaryService.uploadFile(audioFile.getBytes(), "video", "speaking_session_" + sessionId + "_" + System.currentTimeMillis());
            } catch (Exception uploadEx) {
                log.warn("Lỗi upload audio lên Cloudinary, bỏ qua lưu audio", uploadEx);
            }

            List<MessageDto> history = objectMapper.readValue(historyJson, new TypeReference<List<MessageDto>>() {
            });

            history.add(new MessageDto("user", trancript, audioUrl));

            String updateHistoryJson = objectMapper.writeValueAsString(history);
            redisTemplate.opsForValue().set(redisKey, updateHistoryJson, Duration.ofHours(2));

            return new AudioInputResponse(trancript);
        } catch (Exception e) {
            log.error("Lỗi xử lý audio session {}", sessionId, e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }

    public SessionEvaluationResponse endSession(Long sessionId, EndSessionRequest request) {
        SpeakingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ErrorCode.SESSION_NOT_FOUND.toException());

        String redisKey = "speaking:session:" + sessionId;
        String historyJson = (String) redisTemplate.opsForValue().get(redisKey);

        if (historyJson == null) {
            throw ErrorCode.SESSION_NOT_FOUND.toException(); // Không tìm thấy session trên RAM
        }

        try {
            List<MessageDto> history = objectMapper.readValue(historyJson, new TypeReference<>() {});
            
            StringBuilder transcriptBuilder = new StringBuilder();
            List<SpeakingTurn> turns = new ArrayList<>();
            short turnIndex = 1;
            int fillerWordsCount = 0;
            int totalWords = 0;
            
            for (MessageDto msg : history) {
                if ("system".equals(msg.getRole())) continue; // Bỏ qua system prompt

                SpeakerRole role = "user".equals(msg.getRole()) ? SpeakerRole.STUDENT : SpeakerRole.AI;
                
                SpeakingTurn turn = SpeakingTurn.builder()
                        .session(session)
                        .turnIndex(turnIndex)
                        .speaker(role)
                        .transcriptText(msg.getContent())
                        .audioUrl(msg.getAudioUrl())
                        .build();
                turns.add(turn);
                
                if (role == SpeakerRole.STUDENT) {
                    transcriptBuilder.append("[Turn ").append(turnIndex).append("] Student: ").append(msg.getContent()).append("\n");
                    
                    // Logic tính độ trôi chảy (Fluency) cơ bản
                    String text = msg.getContent().toLowerCase();
                    String[] words = text.split("\\s+");
                    totalWords += words.length;
                    for (String word : words) {
                        // Đếm từ thừa (filler words)
                        if (word.matches("um|uh|like|well")) {
                            fillerWordsCount++;
                        }
                    }
                } else {
                    transcriptBuilder.append("[Turn ").append(turnIndex).append("] AI: ").append(msg.getContent()).append("\n");
                }
                turnIndex++;
            }
            
            // Tính điểm Fluency Score
            short fluencyScore = 100;
            if (request.getTotalSpeakingTimeSeconds() != null && request.getTotalSpeakingTimeSeconds() > 0) {
                double minutes = request.getTotalSpeakingTimeSeconds() / 60.0;
                double wpm = totalWords / minutes;
                if (wpm < 80) fluencyScore -= (short) (80 - wpm); // Trừ điểm nếu nói chậm dưới 80 WPM
                fluencyScore -= (short) (fillerWordsCount * 2); // Trừ 2 điểm cho mỗi từ thừa
                if (fluencyScore < 0) fluencyScore = 0;
                if (fluencyScore > 100) fluencyScore = 100;
            }

            // Gọi AI để chấm điểm
            EvaluationResultDto aiEval = aiTextService.evaluateSpeakingSession(transcriptBuilder.toString(), session.getScenario().getGoalDescription());

            // Gắn lại nhận xét của AI vào các Turn tương ứng của User
            if (aiEval != null && aiEval.getTurnsEvaluation() != null) {
                for (EvaluationResultDto.TurnEvaluation eval : aiEval.getTurnsEvaluation()) {
                    for (SpeakingTurn turn : turns) {
                        if (turn.getTurnIndex().equals(eval.getTurnIndex()) && turn.getSpeaker() == SpeakerRole.STUDENT) {
                            if (eval.getGrammarErrors() != null && !eval.getGrammarErrors().isEmpty()) {
                                turn.setGrammarErrorsJson(objectMapper.writeValueAsString(eval.getGrammarErrors()));
                            }
                            if (eval.getVocabularySuggestions() != null && !eval.getVocabularySuggestions().isEmpty()) {
                                turn.setVocabularySuggestionsJson(objectMapper.writeValueAsString(eval.getVocabularySuggestions()));
                            }
                            break;
                        }
                    }
                }
            }

            // Lưu toàn bộ Turns vào DB (Bulk Insert)
            speakingTurnRepository.saveAll(turns);

            // Cập nhật trạng thái Session
            session.setEndedAt(LocalDateTime.now());
            session.setFluencyScore(fluencyScore);
            if (aiEval != null) {
                session.setTaskCompletionScore(aiEval.getTaskCompletionScore());
                session.setIntonationScore(aiEval.getIntonationScore());
                session.setEvaluationJson(objectMapper.writeValueAsString(aiEval.getGeneralFeedback()));
            }
            sessionRepository.save(session);

            // Dọn dẹp RAM
            redisTemplate.delete(redisKey);

            return SessionEvaluationResponse.builder()
                    .fluencyScore(session.getFluencyScore())
                    .taskCompletionScore(session.getTaskCompletionScore())
                    .intonationScore(session.getIntonationScore())
                    .evaluationJson(session.getEvaluationJson())
                    .build();

        } catch (Exception e) {
            log.error("Lỗi khi kết thúc session", e);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }
    }
}
