package com.example.english_app.service.speaking;

import com.example.english_app.exception.ErrorCode;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Measured audio features. Scores are deliberately separated from uncalibrated features.
 */
@Service
public class AudioMetricsService {

    private static final Pattern WORD = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");
    // 'like' and 'well' cannot be classified as fillers from token identity alone.
    private static final Pattern FILLER = Pattern.compile("(?i)\\b(?:um+|uh+|erm+|er+)\\b");

    public static String detectMime(byte[] bytes) {
        if (bytes.length < 12) {
            throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
        }
        if (ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WAVE")) {
            return "audio/wav";
        }
        if (bytes[0] == 0x1a && bytes[1] == 0x45 && (bytes[2] & 255) == 0xdf && (bytes[3] & 255) == 0xa3) {
            return "audio/webm";
        }
        if (ascii(bytes, 0, 4).equals("OggS")) {
            return "audio/ogg";
        }
        if (ascii(bytes, 4, 4).equals("ftyp")) {
            return "audio/mp4";
        }
        if (ascii(bytes, 0, 3).equals("ID3") || ((bytes[0] & 255) == 255 && (bytes[1] & 224) == 224)) {
            return "audio/mpeg";
        }
        throw ErrorCode.UNSUPPORTED_AUDIO_FORMAT.toException();
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    public Map<String, Object> analyze(byte[] audio, String transcript, Double providerDuration) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("metricsVersion", "1");
        int words = (int) WORD.matcher(transcript).results().count();
        int fillers = (int) FILLER.matcher(transcript).results().count();

        metrics.put("wordCount", words);
        metrics.put("fillerCount", fillers);
        metrics.put("fillerMethod", "lexical_um_uh_er_erm_only");
        metrics.put("intonationStatus", "UNAVAILABLE");
        metrics.put("pauseStatus", "UNAVAILABLE");
        metrics.put("measurementSource", audio == null ? "UNAVAILABLE" : "STT_PROVIDER");

        if (providerDuration != null && Double.isFinite(providerDuration) && providerDuration > 0) {
            metrics.put("durationSeconds", providerDuration);
            metrics.put("durationSource", "STT_PROVIDER");
        }

        if (audio != null) {
            try {
                measurePcm(audio, metrics);
            } catch (Exception ignored) {
                metrics.put("audioAnalysisStatus", "UNSUPPORTED_OR_CORRUPT_AUDIO");
            }
        } else {
            metrics.put("audioAnalysisStatus", "NO_AUDIO");
        }

        Object duration = metrics.get("durationSeconds");
        if (duration instanceof Number d && d.doubleValue() > 0 && words > 0) {
            metrics.put("wpm", 60.0 * words / d.doubleValue());
            metrics.put("fluencyStatus", "MEASURED");
        } else {
            metrics.put("fluencyStatus", "INSUFFICIENT_DATA");
        }

        return metrics;
    }

    private void measurePcm(byte[] audio, Map<String, Object> metrics) throws Exception {
        try (AudioInputStream source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
            var format = source.getFormat();
            if (format.getSampleRate() < 8000 || format.getSampleRate() > 48000 || format.getChannels() > 2) {
                throw new IOException("Unsupported audio");
            }

            var pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false
            );

            try (var decoded = AudioSystem.getAudioInputStream(pcm, source)) {
                int maxBytes = (int) (pcm.getFrameRate() * pcm.getFrameSize() * 180);
                byte[] bytes = decoded.readNBytes(maxBytes + 1);
                if (bytes.length > maxBytes || bytes.length < pcm.getFrameSize() * 100) {
                    throw new IOException("Invalid duration");
                }

                int frames = bytes.length / pcm.getFrameSize();
                double duration = frames / (double) pcm.getSampleRate();
                metrics.put("durationSeconds", duration);
                metrics.put("durationSource", "DECODED_AUDIO");
                metrics.put("measurementSource", "DECODED_AUDIO");

                int step = Math.max(1, (int) pcm.getSampleRate() / 8000);
                double sampleRate = pcm.getSampleRate() / step;
                double[] samples = new double[frames / step];

                for (int i = 0; i < samples.length; i++) {
                    double sample = 0;
                    for (int ch = 0; ch < pcm.getChannels(); ch++) {
                        int at = i * step * pcm.getFrameSize() + ch * 2;
                        sample += (short) ((bytes[at] & 255) | (bytes[at + 1] << 8)) / 32768.0;
                    }
                    samples[i] = sample / pcm.getChannels();
                }

                int window = (int) (sampleRate * 0.04);
                List<Map<String, Double>> contour = new ArrayList<>();
                boolean heardSpeech = false;
                double silence = 0;
                int pauses = 0;
                double pauseSeconds = 0;

                for (int offset = 0; offset + window <= samples.length; offset += window) {
                    double energy = 0;
                    for (int k = 0; k < window; k++) {
                        energy += samples[offset + k] * samples[offset + k];
                    }

                    if (Math.sqrt(energy / window) < 0.012) {
                        if (heardSpeech) {
                            silence += 0.04;
                        }
                        continue;
                    }

                    if (heardSpeech && silence >= 0.3) {
                        pauses++;
                        pauseSeconds += silence;
                    }
                    heardSpeech = true;
                    silence = 0;

                    double frequency = pitch(samples, offset, window, sampleRate);
                    if (frequency > 0) {
                        contour.add(Map.of("seconds", offset / sampleRate, "hz", frequency));
                    }
                }

                metrics.put("pauseCount", pauses);
                metrics.put("pauseSeconds", pauseSeconds);
                metrics.put("pauseStatus", "MEASURED");
                metrics.put("pauseMethod", "rms_0.012_internal_silence_300ms_v1");
                metrics.put("pitchContour", contour);
                metrics.put("audioAnalysisStatus", heardSpeech ? "MEASURED" : "NO_SPEECH");
                metrics.put("intonationStatus", contour.size() >= 5 ? "MEASURED_UNCALIBRATED" : "INSUFFICIENT_VOICING");

                // This is a feature, not a CEFR grade or sentence-level correctness verdict.
                if (contour.size() >= 5) {
                    int n = Math.min(5, contour.size() / 2);
                    double first = contour.subList(0, n).stream()
                            .mapToDouble(p -> p.get("hz"))
                            .average()
                            .orElseThrow();
                    double last = contour.subList(contour.size() - n, contour.size()).stream()
                            .mapToDouble(p -> p.get("hz"))
                            .average()
                            .orElseThrow();
                    metrics.put("pitchChangeSemitones", 12 * Math.log(last / first) / Math.log(2));
                }
            }
        }
    }

    private double pitch(double[] samples, int start, int length, double rate) {
        double best = 0;
        int bestLag = 0;
        for (int lag = (int) (rate / 400); lag <= rate / 70 && lag < length; lag++) {
            double cross = 0;
            double a = 0;
            double b = 0;
            for (int k = 0; k < length - lag; k++) {
                double x = samples[start + k];
                double y = samples[start + k + lag];
                cross += x * y;
                a += x * x;
                b += y * y;
            }
            double correlation = cross / Math.sqrt(Math.max(1e-12, a * b));
            if (correlation > best) {
                best = correlation;
                bestLag = lag;
            }
        }
        return (best >= 0.65 && bestLag > 0) ? rate / bestLag : 0;
    }
}
