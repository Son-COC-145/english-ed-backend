package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.service.integration.TtsGenerationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpeakingWorker {

    private final SpeakingStore store;
    private final SpeakingJson json;
    private final SpeakingAiClient ai;
    private final AudioMetricsService metrics;
    private final TtsGenerationService tts;

    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private final AtomicInteger active = new AtomicInteger();

    @Scheduled(fixedDelayString = "${speaking.worker.poll-ms:500}")
    public void poll() {
        while (active.get() < 4) {
            SpeakingStore.Job job = store.claim();
            if (job == null) {
                return;
            }
            active.incrementAndGet();
            workers.submit(() -> {
                try {
                    log.info("Speaking job started jobId={} sessionId={} turnId={} kind={} attempt={} lease={}",
                            job.id(), job.sessionId(), job.turnId(), job.kind(), job.attempts(), job.token());
                    process(job);
                    log.info("Speaking job handled jobId={} sessionId={} attempt={}", job.id(), job.sessionId(), job.attempts());
                } catch (Exception e) {
                    log.warn("Speaking job failed jobId={} sessionId={} kind={} attempt={} error={}",
                            job.id(), job.sessionId(), job.kind(), job.attempts(), e.getClass().getSimpleName());
                    try {
                        store.failed(job, e);
                    } catch (Exception persistenceFailure) {
                        log.error("Cannot persist job failure jobId={} error={}; lease recovery will retry",
                                job.id(), persistenceFailure.getClass().getSimpleName());
                    }
                } finally {
                    active.decrementAndGet();
                }
            });
        }
    }

    void process(SpeakingStore.Job job) throws Exception {
        if (store.exhausted(job)) {
            store.failed(job, new IllegalStateException("Retry budget exhausted after lease expiry"));
            return;
        }
        JsonNode scenario = json.read(store.session(job.sessionId()).getScenarioSnapshotJson());

        switch (job.kind()) {
            case "INPUT" -> {
                SpeakingTurn turn = store.turn(job.turnId());
                String transcript = turn.getTranscriptText();
                Double duration = null;

                if (turn.getAudioData() != null) {
                    SpeakingAiClient.Transcription result = ai.transcribe(turn.getAudioData(), turn.getAudioContentType());
                    transcript = result.text();
                    duration = result.durationSeconds();
                }

                store.inputDone(job, transcript, json.encode(metrics.analyze(turn.getAudioData(), transcript, duration)));
            }
            case "RESPONSE" -> {
                SpeakingTurn turn = store.turn(job.turnId());
                // Persisted text is reusable if a previous attempt only failed in TTS.
                String text = turn.getTranscriptText();

                if (!turn.isResponseTextReady()) {
                    List<SpeakingTurn> history = store.history(job.sessionId()).stream()
                            .filter(t -> t.getTurnIndex() < turn.getTurnIndex())
                            .toList();

                    text = ai.reply(scenario, history, partial -> {
                        if (!store.progress(job, partial)) {
                            throw new IllegalStateException("Lease lost");
                        }
                    });
                    if (!store.textReady(job, text)) return;
                }

                byte[] audio = tts.generateAudioStream(text, null);
                if (audio == null || audio.length == 0) {
                    throw new IllegalStateException("Empty TTS audio");
                }
                store.responseDone(job, text, audio);
            }
            case "TURN_EVALUATION" -> {
                SpeakingTurn turn = store.turn(job.turnId());
                JsonNode evaluation = ai.evaluateTurn(turn, scenario.path("cefr").asText());
                store.evaluationDone(job, evaluation);
            }
            case "SESSION_EVALUATION" -> {
                List<SpeakingTurn> history = store.history(job.sessionId());
                List<SpeakingTurn> studentTurns = history.stream()
                        .filter(t -> t.getSpeaker() == SpeakerRole.STUDENT)
                        .toList();

                if (history.stream().anyMatch(t -> "FAILED".equals(t.getStatus()))
                        || studentTurns.stream().anyMatch(t -> "FAILED".equals(t.getEvaluationStatus()))) {
                    store.reportBlocked(job);
                    return;
                }

                if (history.stream().anyMatch(t -> !"COMPLETED".equals(t.getStatus()))
                        || studentTurns.stream().anyMatch(t -> !"COMPLETED".equals(t.getEvaluationStatus()))) {
                    store.defer(job);
                    return;
                }

                ObjectNode report = (ObjectNode) ai.evaluateGoals(scenario, history);
                int words = 0;
                int fillers = 0;
                int pauses = 0;
                double seconds = 0;
                boolean completeDuration = true;
                boolean completePauses = true;

                for (SpeakingTurn turn : studentTurns) {
                    JsonNode m = json.read(turn.getAudioMetricsJson());
                    words += m.path("wordCount").asInt();
                    fillers += m.path("fillerCount").asInt();

                    if (!m.has("durationSeconds")) {
                        completeDuration = false;
                    } else {
                        seconds += m.path("durationSeconds").asDouble();
                    }

                    if (!m.has("pauseCount")) {
                        completePauses = false;
                    } else {
                        pauses += m.path("pauseCount").asInt();
                    }
                }

                ObjectNode fluency = report.putObject("fluency");
                fluency.put("wordCount", words);
                fluency.put("fillerCount", fillers);
                fluency.put("cefr", scenario.path("cefr").asText());
                fluency.put("scoreStatus", "RUBRIC_NOT_CALIBRATED");

                if (completeDuration && seconds > 0 && words > 0) {
                    fluency.put("durationSeconds", seconds);
                    fluency.put("wpm", 60.0 * words / seconds);
                } else {
                    fluency.put("measurementStatus", "INSUFFICIENT_DATA");
                    fluency.put("scoreStatus", "INSUFFICIENT_DATA");
                }

                if (completePauses) {
                    fluency.put("pauseCount", pauses);
                } else {
                    fluency.put("pauseStatus", "UNAVAILABLE");
                }

                report.put("intonationStatus", "NOT_ASSESSED");
                store.reportDone(job, report, null, null);
            }
            default -> throw new IllegalStateException("Unknown speaking job kind: " + job.kind());
        }
    }

    @PreDestroy
    public void close() {
        workers.shutdownNow();
    }
}
