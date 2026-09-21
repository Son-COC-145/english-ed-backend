package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only subscription: reconnecting never schedules an AI request.
 * Events carry replaceable turn snapshots, including on retry/reconnect.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiStreamingService {

    private final SpeakingSessionService sessions;
    private final SpeakingAccess access;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore clients = new Semaphore(64);

    public SseEmitter streamResponse(Long id) {
        Long userId = access.userId();
        sessions.checkOwnership(id, userId);

        if (!clients.tryAcquire()) {
            throw ErrorCode.SPEAKING_UNAVAILABLE.toException();
        }

        var emitter = new SseEmitter(120_000L);
        var stopped = new AtomicBoolean();

        emitter.onCompletion(() -> stopped.set(true));
        emitter.onTimeout(() -> stopped.set(true));
        emitter.onError(e -> stopped.set(true));

        executor.submit(() -> {
            try {
                Map<Long, String> sent = new HashMap<>();
                Set<Long> textFinalSent = new HashSet<>();
                Map<Long, String> audioSent = new HashMap<>();
                String sessionSignature = null;
                long nextHeartbeat = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(115);

                while (!stopped.get() && System.nanoTime() < deadline) {
                    var snapshot = sessions.streamingSnapshot(id, userId);
                    var report = snapshot.report();
                    var finalizedAiTurns = snapshot.finalizedAiTurnIds();
                    String nextSessionSignature = report.getStatus();
                    if (!nextSessionSignature.equals(sessionSignature)) {
                        emitter.send(SseEmitter.event().name("session").data(
                                Map.of("sessionId", id, "status", report.getStatus())));
                        sessionSignature = nextSessionSignature;
                    }

                    for (var turn : report.getTurns()) {
                        String signature = objectMapper.writeValueAsString(turn);
                        if (!signature.equals(sent.put(turn.getId(), signature))) {
                            String event = turn.getSpeaker() == SpeakerRole.STUDENT ? "transcript" : "text";
                            emitter.send(SseEmitter.event().name(event).data(turn));
                        }
                        if (turn.getSpeaker() == SpeakerRole.AI && finalizedAiTurns.contains(turn.getId())
                                && textFinalSent.add(turn.getId())) {
                            emitter.send(SseEmitter.event().name("text-final").data(turn));
                        }
                        if ("COMPLETED".equals(turn.getStatus()) && turn.getAudioUrl() != null
                                && turn.getSpeaker() == SpeakerRole.AI
                                && !turn.getAudioUrl().equals(audioSent.put(turn.getId(), turn.getAudioUrl()))) {
                            emitter.send(SseEmitter.event().name("audio").data(
                                    Map.of("turnId", turn.getId(), "audioUrl", turn.getAudioUrl())));
                        }
                    }

                    var failedTurn = report.getTurns().stream().filter(t -> "FAILED".equals(t.getStatus()))
                            .findFirst().orElse(null);
                    var last = report.getTurns().isEmpty() ? null : report.getTurns().getLast();

                    if (failedTurn != null || (last != null && last.getSpeaker() == SpeakerRole.AI && "COMPLETED".equals(last.getStatus()))) {
                        if (failedTurn != null) {
                            emitter.send(SseEmitter.event().name("error").data(Map.of(
                                    "sessionId", id, "turnId", failedTurn.getId(),
                                    "errorCode", failedTurn.getErrorCode() == null ? "PROCESSING_FAILED" : failedTurn.getErrorCode())));
                        } else {
                            emitter.send(SseEmitter.event().name("done").data(Map.of("sessionId", id, "turnId", last.getId())));
                        }
                        break;
                    }

                    if (System.nanoTime() >= nextHeartbeat) {
                        emitter.send(SseEmitter.event().comment("heartbeat"));
                        nextHeartbeat = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
                    }

                    Thread.sleep(300);
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("Error streaming speaking session response for sessionId={}", id, e);
                if (!stopped.get()) {
                    emitter.completeWithError(e);
                }
            } finally {
                clients.release();
            }
        });

        return emitter;
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
