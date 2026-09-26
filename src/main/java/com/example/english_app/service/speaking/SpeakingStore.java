package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.SpeakingScenario;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.entity.speaking.SpeakingStartRequest;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.speaking.SpeakingStartRequestRepository;
import com.example.english_app.repository.speaking.SpeakingTurnRepository;
import com.example.english_app.repository.speaking.SpeakingJobRepository;
import com.example.english_app.repository.speaking.SpeakingRewardRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Short database transactions only; all provider calls run outside these methods.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SpeakingStore {

    private final SpeakingSessionRepository sessions;
    private final SpeakingScenarioRepository scenarios;
    private final SpeakingStartRequestRepository startRequests;
    private final SpeakingTurnRepository turns;
    private final UserRepository users;
    private final SpeakingJobRepository jobs;
    private final SpeakingRewardRepository rewards;
    private final SpeakingJson json;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    public SpeakingSession owned(Long id, Long userId) {
        SpeakingSession s = sessions.findById(id).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        if (!s.getStudent().getId().equals(userId)) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }
        return s;
    }

    private SpeakingSession lockOwned(Long id, Long userId) {
        SpeakingSession s = sessions.lockById(id).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        if (!s.getStudent().getId().equals(userId)) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }
        return s;
    }

    private void ongoing(SpeakingSession s) {
        if (!"ONGOING".equals(s.getStatus())) {
            throw ErrorCode.SPEAKING_CONFLICT.toException();
        }
    }

    public List<SpeakingTurn> history(Long id) {
        return turns.findBySessionIdOrderByTurnIndexAscIdAsc(id);
    }

    public SpeakingSession recover(Long userId, String key) {
        return startRequests.findByStudentIdAndRequestKey(userId, key)
                .map(SpeakingStartRequest::getSession)
                .filter(s -> s.getStudent().getId().equals(userId))
                .orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
    }

    public List<SpeakingSession> activeSessions(Long userId) {
        return sessions.findTop20ByStudentIdAndStatusInOrderByStartedAtDescIdDesc(userId,
                List.of("ONGOING", "EVALUATING", "EVALUATION_FAILED"));
    }

    public SpeakingSession start(Long userId, Short scenarioId, String requestKey) {
        int claimed = startRequests.claim(userId, requestKey, scenarioId);
        if (claimed == 0) {
            var existing = startRequests.findByStudentIdAndRequestKey(userId, requestKey)
                    .orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
            if (!scenarioId.equals(existing.getScenario().getId()) || existing.getSession() == null) {
                throw ErrorCode.SPEAKING_CONFLICT.toException();
            }
            return sessions.findById(existing.getSession().getId())
                    .orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        }

        SpeakingScenario scenario = scenarios.findById(scenarioId)
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .filter(s -> s.getTopic() == null || Boolean.TRUE.equals(s.getTopic().getIsActive()))
                .orElseThrow(ErrorCode.SCENARIO_NOT_FOUND::toException);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("prompt", scenario.getAiSystemPrompt());
        snapshot.put("context", scenario.getContextDescription());
        snapshot.put("persona", scenario.getAiRoleName());
        snapshot.put("goal", scenario.getGoalDescription());
        snapshot.put("cefr", scenario.getCefrLevel().name());
        snapshot.put("hints", json.read(scenario.getHintPhrasesJson()));

        SpeakingSession s = sessions.saveAndFlush(SpeakingSession.builder()
                .student(users.getReferenceById(userId))
                .scenario(scenario)
                .scenarioSnapshotJson(json.encode(snapshot))
                .build());

        SpeakingTurn greeting = turns.saveAndFlush(SpeakingTurn.builder()
                .session(s)
                .turnIndex(1)
                .speaker(SpeakerRole.AI)
                .transcriptText("")
                .evaluationStatus("NOT_APPLICABLE")
                .build());

        enqueue(s.getId(), greeting.getId(), "RESPONSE");
        startRequests.bindSession(userId, requestKey, s);
        return s;
    }

    public void attachAudioUrl(Long id, Long userId, Long turnId, String audioUrl) {
        SpeakingSession session = lockOwned(id, userId);
        SpeakingTurn turn = turns.findById(turnId)
                .filter(t -> t.getSession().getId().equals(session.getId()))
                .orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        if (turn.getAudioUrl() == null || turn.getAudioUrl().isBlank()) {
            turn.setAudioUrl(audioUrl);
        }
    }

    public SpeakingTurn submit(Long id, Long userId, String key, String hash, byte[] audio, String mime, String text) {
        return submit(id, userId, key, hash, audio, mime, text, null);
    }

    public SpeakingTurn submit(Long id, Long userId, String key, String hash, byte[] audio, String mime, String text, String audioUrl) {
        SpeakingSession s = lockOwned(id, userId);
        var existing = turns.findBySessionIdAndRequestKey(id, key);
        if (existing.isPresent()) {
            if (!hash.equals(existing.get().getInputHash())) {
                throw ErrorCode.SPEAKING_CONFLICT.toException();
            }
            return existing.get();
        }

        ongoing(s);
        List<SpeakingTurn> history = history(id);
        if (history.size() >= 201 || history.stream().anyMatch(t -> !"COMPLETED".equals(t.getStatus()))) {
            throw ErrorCode.SPEAKING_CONFLICT.toException();
        }

        int nextTurnIndex = history.stream()
                .mapToInt(SpeakingTurn::getTurnIndex)
                .max()
                .orElse(0) + 1;

        SpeakingTurn t = turns.saveAndFlush(SpeakingTurn.builder()
                .session(s)
                .turnIndex(nextTurnIndex)
                .speaker(SpeakerRole.STUDENT)
                .requestKey(key)
                .inputHash(hash)
                .audioData(audio)
                .audioContentType(mime)
                .audioUrl(audioUrl)
                .recordedAt(audio == null ? null : LocalDateTime.now())
                .audioAnalysisStatus(audio == null ? "INSUFFICIENT_DATA" : "PENDING")
                .audioStatus(audio == null ? "NOT_APPLICABLE" : "READY")
                .transcriptText(text == null ? "" : text)
                .build());

        enqueue(id, t.getId(), "INPUT");
        return t;
    }

    public JsonNode hint(Long id, Long userId, String key) {
        SpeakingSession s = lockOwned(id, userId);
        JsonNode hints = json.read(s.getScenarioSnapshotJson()).path("hints");
        if (!hints.isArray() || hints.size() < 3 || hints.size() > 5) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }

        ArrayNode keys = (ArrayNode) json.read(s.getHintRequestsJson());
        if (StreamSupport.stream(keys.spliterator(), false).anyMatch(n -> n.asText().equals(key))) {
            return hints;
        }

        ongoing(s);
        if (keys.size() >= 100) {
            throw ErrorCode.SPEAKING_CONFLICT.toException();
        }

        keys.add(key);
        s.setHintRequestsJson(json.encode(keys));
        s.setHintUsedCount((short) keys.size());
        return hints;
    }

    public SpeakingSession end(Long id, Long userId) {
        SpeakingSession s = lockOwned(id, userId);
        if (!"ONGOING".equals(s.getStatus())) {
            return s;
        }

        List<SpeakingTurn> history = history(id);
        if (history.stream().noneMatch(t -> t.getSpeaker() == SpeakerRole.STUDENT)) {
            throw ErrorCode.SPEAKING_CONFLICT.toException();
        }

        s.setStatus("EVALUATING");
        s.setEndedAt(LocalDateTime.now());
        enqueue(id, null, "SESSION_EVALUATION");
        return s;
    }

    public void retry(Long id, Long userId) {
        retry(id, userId, null);
    }

    public void retry(Long id, Long userId, Long turnId) {
        SpeakingSession s = lockOwned(id, userId);
        if (turnId != null && history(id).stream().noneMatch(t -> t.getId().equals(turnId))) {
            throw ErrorCode.SESSION_NOT_FOUND.toException();
        }
        if ("COMPLETED".equals(s.getStatus())) {
            return;
        }

        jobs.resetFailed(id, turnId);
        for (SpeakingTurn t : history(id)) {
            if (turnId != null && !turnId.equals(t.getId())) continue;
            if ("FAILED".equals(t.getStatus())) {
                t.setStatus("PENDING");
                t.setErrorCode(null);
            }
            if (turnId == null || turnId.equals(t.getId())) {
                if (t.getSpeaker() == SpeakerRole.AI && "FAILED".equals(t.getAudioStatus())) {
                    t.setAudioStatus("PENDING");
                    t.setAudioErrorCode(null);
                }
                if (t.getSpeaker() == SpeakerRole.STUDENT && "FAILED".equals(t.getAudioAnalysisStatus())) {
                    t.setAudioAnalysisStatus("PENDING");
                    t.setAudioErrorCode(null);
                }
            }
            if ("FAILED".equals(t.getEvaluationStatus())) {
                t.setEvaluationStatus("PENDING");
                t.setErrorCode(null);
            }
        }
        if ("EVALUATION_FAILED".equals(s.getStatus()) || "FAILED".equals(s.getStatus())) {
            s.setStatus("EVALUATING");
        }
    }

    private void enqueue(Long sessionId, Long turnId, String kind) {
        jobs.enqueue(sessionId, turnId, kind);
        publisher.publishEvent(new SpeakingJobCreatedEvent(this));
    }

    public record Job(
            long id,
            long sessionId,
            Long turnId,
            String kind,
            int attempts,
            String token
    ) {}

    public Job claim() {
        var candidate = jobs.findEligibleForUpdate();
        if (candidate.isEmpty()) return null;
        var job = candidate.get();
        int attempt = job.getAttempts() + 1;
        String token = UUID.randomUUID().toString();
        if ("RUNNING".equals(job.getStatus())) jobs.expireAttempts(job.getId());
        jobs.acquireLease(job.getId(), token);
        jobs.recordAttempt(job.getId(), attempt, token);
        return new Job(job.getId(), job.getSessionId(), job.getTurnId(), job.getKind(), attempt, token);
    }

    private boolean current(Job j) {
        // Every mutation locks session before job, including /end and /retry.
        sessions.lockById(j.sessionId()).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        return jobs.findCurrentForUpdate(j.id(), j.token()).isPresent();
    }

    public boolean exhausted(Job j) {
        return j.attempts() > jobs.maxAttempts(j.id());
    }

    public SpeakingSession session(Long id) {
        return sessions.findById(id).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
    }

    public SpeakingTurn turn(Long id) {
        return turns.findById(id).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
    }

    public boolean progress(Job j, String text) {
        if (!current(j)) {
            return false;
        }
        turn(j.turnId()).setTranscriptText(text);
        return true;
    }

    public void inputDone(Job j, String transcript, String metrics) {
        if (!current(j)) {
            return;
        }
        SpeakingSession s = sessions.lockById(j.sessionId()).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        SpeakingTurn t = turn(j.turnId());
        t.setTranscriptText(transcript);
        t.setAudioMetricsJson(metrics);
        JsonNode metricNode = json.read(metrics);
        t.setDurationSeconds(metricNode.has("durationSeconds") ? metricNode.path("durationSeconds").asDouble() : null);
        t.setMetricsVersion(metricNode.path("metricsVersion").asText("1"));
        t.setAudioAnalysisStatus(metricNode.path("audioAnalysisStatus").asText(
                metricNode.has("durationSeconds") ? "MEASURED" : "INSUFFICIENT_DATA"));
        t.setStatus("COMPLETED");

        SpeakingTurn reply = turns.saveAndFlush(SpeakingTurn.builder()
                .session(s)
                .turnIndex(t.getTurnIndex() + 1)
                .speaker(SpeakerRole.AI)
                .transcriptText("")
                .evaluationStatus("NOT_APPLICABLE")
                .build());

        enqueue(j.sessionId(), reply.getId(), "RESPONSE");
        enqueue(j.sessionId(), t.getId(), "TURN_EVALUATION");
        done(j);
    }

    public boolean textReady(Job j, String text) {
        if (!current(j)) {
            return false;
        }
        SpeakingTurn t = turn(j.turnId());
        t.setTranscriptText(text);
        t.setResponseTextReady(true);
        return true;
    }

    public void reportBlocked(Job j) {
        if (!current(j)) {
            return;
        }
        jobs.blockReport(j.id());
        finishAttempt(j, "FAILED", "TURN_PROCESSING_FAILED", "A required turn failed", true);
        sessions.lockById(j.sessionId()).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException).setStatus("EVALUATION_FAILED");
    }

    public void responseDone(Job j, String text, byte[] audio) {
        responseDone(j, text, audio, null);
    }

    public void responseDone(Job j, String text, byte[] audio, String audioUrl) {
        if (!current(j)) {
            return;
        }
        SpeakingTurn t = turn(j.turnId());
        t.setTranscriptText(text);
        t.setAudioData(audio);
        t.setAudioUrl(audioUrl);
        t.setAudioContentType("audio/mpeg");
        t.setAudioStatus("READY");
        t.setStatus("COMPLETED");
        t.setErrorCode(null);
        done(j);
    }

    public void evaluationDone(Job j, JsonNode evaluation) {
        if (!current(j)) {
            return;
        }
        SpeakingTurn t = turn(j.turnId());
        t.setGrammarErrorsJson(json.encode(evaluation.path("grammar_errors")));
        t.setVocabularySuggestionsJson(json.encode(evaluation.path("vocabulary_suggestions")));
        t.setEvaluationStatus("COMPLETED");
        t.setErrorCode(null);
        done(j);
    }

    public void reportDone(Job j, JsonNode report, Short fluency, Short intonation) {
        if (!current(j)) {
            return;
        }
        SpeakingSession s = sessions.lockById(j.sessionId()).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException);
        if ("COMPLETED".equals(s.getStatus())) {
            done(j);
            return;
        }

        s.setEvaluationJson(json.encode(report));
        s.setFluencyScore(fluency);
        s.setIntonationScore(intonation);
        s.setTaskCompletionScore((short) report.path("task_completion_score").asInt());

        short xp = (short) (20 + Math.max(0, 10 - s.getHintUsedCount() * 2));
        if (rewards.insertOnce(s.getId(), s.getStudent().getId(), xp) > 0) {
            rewards.incrementXp(s.getStudent().getId(), xp);
        }

        s.setXpEarned(xp);
        s.setStatus("COMPLETED");
        done(j);
    }

    public void defer(Job j) {
        if (current(j)) {
            jobs.defer(j.id());
            finishAttempt(j, "DEFERRED", null, null, true);
        }
    }

    public void failed(Job j, Exception failure) {
        if (!current(j)) {
            return;
        }
        int limit = jobs.maxAttempts(j.id());
        boolean noSpeech = failure instanceof AppException app
                && app.getErrorCode() == ErrorCode.SPEAKING_AUDIO_NO_SPEECH;
        boolean terminal = noSpeech || j.attempts() >= limit;
        String code = failure instanceof AppException app
                ? app.getErrorCode().name()
                : failure instanceof IllegalArgumentException ? "INVALID_AI_OUTPUT" : "PROVIDER_OR_PROCESSING_ERROR";
        // Exception messages may contain provider payloads or credentials. Store only the class.
        String message = failure.getClass().getSimpleName();
        int delay = Math.min(300, 10 * (1 << Math.min(j.attempts() - 1, 5)));
        jobs.updateFailure(j.id(), terminal ? "FAILED" : "PENDING", terminal, code, message, delay);
        finishAttempt(j, "FAILED", code, message, !terminal);
        if (terminal) {
            if (j.turnId() == null) {
                sessions.lockById(j.sessionId()).orElseThrow(ErrorCode.SESSION_NOT_FOUND::toException).setStatus("EVALUATION_FAILED");
            } else {
                SpeakingTurn t = turn(j.turnId());
                t.setErrorCode(code);
                if ("INPUT".equals(j.kind())) {
                    t.setAudioAnalysisStatus("FAILED");
                } else if ("RESPONSE".equals(j.kind())) {
                    t.setAudioStatus("FAILED");
                    t.setStatus("FAILED");
                    t.setAudioErrorCode(code);
                }
                if ("TURN_EVALUATION".equals(j.kind())) {
                    t.setEvaluationStatus("FAILED");
                } else {
                    t.setStatus("FAILED");
                }
            }
        }
    }

    private void done(Job j) {
        jobs.complete(j.id());
        finishAttempt(j, "SUCCEEDED", null, null, false);
    }

    private void finishAttempt(Job j, String status, String code, String message, boolean retryable) {
        jobs.finishAttempt(j.id(), j.token(), status, code, message, retryable);
    }

}
