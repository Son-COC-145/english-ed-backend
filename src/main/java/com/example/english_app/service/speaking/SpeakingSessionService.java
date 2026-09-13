package com.example.english_app.service.speaking;

import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.dto.response.SpeakingTurnResponse;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.service.integration.CloudinaryService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeakingSessionService {

    private final SpeakingStore store;
    private final SpeakingAccess access;
    private final SpeakingMapper mapper;
    private final SpeakingJson json;
    private final CloudinaryService cloudinary;
    private final SpeakingAudioValidator audioValidator;

    @Transactional
    public SpeakingSessionResponse startSession(StartSessionRequest request, String requestKey) {
        validateKey(requestKey);
        SpeakingSession session = store.start(access.userId(), request.getScenarioId(), requestKey);
        List<SpeakingTurn> history = store.history(session.getId());
        SpeakingSessionResponse response = mapper.toSessionResponse(session, history);
        if (!history.isEmpty()) {
            response.setGreetingTurnId(history.getFirst().getId());
        }
        return response;
    }

    public AudioInputResponse processUserAudio(Long id, String requestKey, MultipartFile file) {
        validateKey(requestKey);
        Long userId = access.userId();
        store.owned(id, userId);

        if (file == null || file.isEmpty()) {
            throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw ErrorCode.AUDIO_PAYLOAD_TOO_LARGE.toException();
        }

        try {
            byte[] data = file.getBytes();
            audioValidator.validate(data);
            String mime = AudioMetricsService.detectMime(data);
            String inputHash = hash(data);
            SpeakingTurn turn = store.submit(id, userId, requestKey, inputHash, data, mime, null);
            if (turn.getAudioUrl() == null || turn.getAudioUrl().isBlank()) {
                try {
                    String audioUrl = cloudinary.uploadFile(data, "video",
                            "speaking/session-" + id + "/student-" + requestKey);
                    store.attachAudioUrl(id, userId, turn.getId(), audioUrl);
                    turn.setAudioUrl(audioUrl);
                } catch (RuntimeException uploadFailure) {
                    // The DB copy remains the canonical fallback; worker can process it without Cloudinary.
                    log.warn("Speaking input audio upload failed sessionId={} turnId={} error={}",
                            id, turn.getId(), uploadFailure.getClass().getSimpleName());
                }
            }
            return new AudioInputResponse(turn.getId(), turn.getStatus(), turn.getTranscriptText());
        } catch (IOException e) {
            throw ErrorCode.AUDIO_PROCESSING_FAILED.toException();
        }
    }

    public AudioInputResponse processText(Long id, String requestKey, String text) {
        validateKey(requestKey);
        if (text == null || text.isBlank() || text.length() > 4000) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        String sanitizedText = text.strip();
        SpeakingTurn turn = store.submit(
                id,
                access.userId(),
                requestKey,
                hash(sanitizedText.getBytes(StandardCharsets.UTF_8)),
                null,
                null,
                sanitizedText
        );
        return new AudioInputResponse(turn.getId(), turn.getStatus(), turn.getTranscriptText());
    }

    @Transactional
    public SessionEvaluationResponse endSession(Long id) {
        Long userId = access.userId();
        store.end(id, userId);
        return reportFor(id, userId);
    }

    @Transactional(readOnly = true)
    public SessionEvaluationResponse report(Long id) {
        return reportFor(id, access.userId());
    }

    @Transactional(readOnly = true)
    public SessionEvaluationResponse reportFor(Long id, Long userId) {
        return streamingSnapshot(id, userId).report();
    }

    public record StreamingSnapshot(SessionEvaluationResponse report, Set<Long> finalizedAiTurnIds) {}

    @Transactional(readOnly = true)
    public StreamingSnapshot streamingSnapshot(Long id, Long userId) {
        SpeakingSession session = store.owned(id, userId);
        List<SpeakingTurn> history = store.history(id);
        List<SpeakingTurnResponse> turns = history.stream()
                .map(this::turnResponse)
                .toList();

        var report = SessionEvaluationResponse.builder()
                .sessionId(id)
                .status(session.getStatus())
                .fluencyScore(session.getFluencyScore())
                .intonationScore(session.getIntonationScore())
                .taskCompletionScore(session.getTaskCompletionScore())
                .xpEarned(session.getXpEarned())
                .hintUsedCount(session.getHintUsedCount())
                .evaluation(json.read(session.getEvaluationJson()))
                .turns(turns)
                .build();
        var finalizedIds = history.stream()
                .filter(t -> t.getSpeaker() == SpeakerRole.AI
                        && (t.isResponseTextReady() || "COMPLETED".equals(t.getStatus())))
                .map(SpeakingTurn::getId).collect(Collectors.toSet());
        return new StreamingSnapshot(report, finalizedIds);
    }

    private SpeakingTurnResponse turnResponse(SpeakingTurn turn) {
        SpeakingTurnResponse response = mapper.toTurnResponse(turn);
        if (turn.getAudioData() != null && (turn.getAudioUrl() == null || turn.getAudioUrl().isBlank())) {
            response.setAudioUrl("/api/v1/speaking-session/" + turn.getSession().getId() + "/turns/" + turn.getId() + "/audio");
        }
        return response;
    }

    @Transactional(readOnly = true)
    public SpeakingTurn audio(Long id, Long turnId) {
        store.owned(id, access.userId());
        SpeakingTurn turn = store.turn(turnId);
        if (!turn.getSession().getId().equals(id)) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }
        if (turn.getSpeaker() == SpeakerRole.AI) {
            if ("FAILED".equals(turn.getAudioStatus()) || "FAILED".equals(turn.getStatus())) {
                throw ErrorCode.SPEAKING_AUDIO_FAILED.toException();
            }
            if (!"READY".equals(turn.getAudioStatus())) throw ErrorCode.SPEAKING_AUDIO_PENDING.toException();
        }
        if (turn.getAudioData() == null || turn.getAudioData().length == 0) {
            throw ErrorCode.SPEAKING_AUDIO_MISSING.toException();
        }
        return turn;
    }

    @Transactional(readOnly = true)
    public SessionEvaluationResponse recover(String startKey) {
        validateKey(startKey);
        return reportFor(store.recover(access.userId(), startKey).getId(), access.userId());
    }

    @Transactional(readOnly = true)
    public List<SpeakingSessionResponse> activeSessions() {
        return store.activeSessions(access.userId()).stream()
                .map(s -> mapper.toSessionResponse(s, store.history(s.getId()))).toList();
    }


    public JsonNode hint(Long id, String key) {
        validateKey(key);
        return store.hint(id, access.userId(), key);
    }

    public void retry(Long id) {
        store.retry(id, access.userId());
    }

    public void retryTurn(Long id, Long turnId) {
        store.retry(id, access.userId(), turnId);
    }

    private void validateKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9_-]{8,100}")) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }

    private String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
