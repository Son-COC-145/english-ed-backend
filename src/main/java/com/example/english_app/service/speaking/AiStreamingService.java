package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.exception.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
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

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Semaphore clients = new Semaphore(64);

    public SseEmitter streamResponse(Long id) {
        Long userId = access.userId();
        sessions.reportFor(id, userId);

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
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(115);

                while (!stopped.get() && System.nanoTime() < deadline) {
                    var report = sessions.reportFor(id, userId);

                    for (var turn : report.getTurns()) {
                        String signature = turn.getStatus() + ":" + turn.getTranscriptText() + ":" + turn.getAudioUrl();
                        if (!signature.equals(sent.put(turn.getId(), signature))) {
                            String event = turn.getSpeaker() == SpeakerRole.STUDENT ? "transcript" : "text";
                            emitter.send(SseEmitter.event().name(event).data(turn));

                            if ("COMPLETED".equals(turn.getStatus())
                                    && turn.getAudioUrl() != null
                                    && turn.getSpeaker() == SpeakerRole.AI) {
                                emitter.send(SseEmitter.event().name("audio").data(
                                        Map.of("turnId", turn.getId(), "audioUrl", turn.getAudioUrl())
                                ));
                            }
                        }
                    }

                    boolean failed = report.getTurns().stream().anyMatch(t -> "FAILED".equals(t.getStatus()));
                    var last = report.getTurns().isEmpty() ? null : report.getTurns().getLast();

                    if (failed || (last != null && last.getSpeaker() == SpeakerRole.AI && "COMPLETED".equals(last.getStatus()))) {
                        emitter.send(SseEmitter.event().name(failed ? "error" : "done").data(Map.of("sessionId", id)));
                        break;
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
