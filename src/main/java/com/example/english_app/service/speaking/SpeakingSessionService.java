package com.example.english_app.service.speaking;

import com.example.english_app.dto.request.StartSessionRequest;
import com.example.english_app.dto.response.AudioInputResponse;
import com.example.english_app.dto.response.SessionEvaluationResponse;
import com.example.english_app.dto.response.SpeakingSessionResponse;
import com.example.english_app.dto.response.SpeakingTurnResponse;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.service.integration.CloudinaryService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeakingSessionService {

    private final SpeakingStore store;
    private final SpeakingAccess access;
    private final SpeakingMapper mapper;
    private final SpeakingJson json;
    private final CloudinaryService cloudinary;

    @Transactional
    public SpeakingSessionResponse startSession(StartSessionRequest request) {
        SpeakingSession session = store.start(access.userId(), request.getScenarioId());
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
            String mime = AudioMetricsService.detectMime(data);
            String audioUrl = cloudinary.uploadFile(data, "video",
                    "speaking/session-" + id + "/student-" + requestKey);
            SpeakingTurn turn = store.submit(id, userId, requestKey, hash(data), data, mime, null, audioUrl);
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
        SpeakingSession session = store.owned(id, userId);
        List<SpeakingTurnResponse> turns = store.history(id).stream()
                .map(this::turnResponse)
                .toList();

        return SessionEvaluationResponse.builder()
                .sessionId(id)
                .status(session.getStatus())
                .fluencyScore(null)
                .intonationScore(null)
                .taskCompletionScore(session.getTaskCompletionScore())
                .xpEarned(session.getXpEarned())
                .hintUsedCount(session.getHintUsedCount())
                .evaluation(json.read(session.getEvaluationJson()))
                .turns(turns)
                .build();
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
        if (!turn.getSession().getId().equals(id) || turn.getAudioData() == null) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }
        return turn;
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
