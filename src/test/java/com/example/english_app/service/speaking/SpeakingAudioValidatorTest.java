package com.example.english_app.service.speaking;

import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class SpeakingAudioValidatorTest {
    private final SpeakingAudioValidator validator = new SpeakingAudioValidator();

    static byte[] wav(double seconds, boolean audible) {
        int samples = (int) (seconds * 16000);
        var buffer = ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(buffer.capacity() - 8);
        buffer.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        buffer.putShort((short) 1).putShort((short) 1).putInt(16000).putInt(32000);
        buffer.putShort((short) 2).putShort((short) 16);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(samples * 2);
        for (int i = 0; i < samples; i++) {
            buffer.putShort(audible ? (short) (8000 * Math.sin(i * 2 * Math.PI * 200 / 16000)) : 0);
        }
        return buffer.array();
    }

    private void rejects(byte[] data, ErrorCode code) {
        assertThatThrownBy(() -> validator.validate(data)).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }

    @Test void acceptsFinalizedRecorderWavAtMinimumDuration() {
        assertThatCode(() -> validator.validate(wav(1, true))).doesNotThrowAnyException();
    }

    @Test void rejectsSilenceBeforeSchedulingProviderWork() {
        rejects(wav(2, false), ErrorCode.SPEAKING_AUDIO_NO_SPEECH);
    }

    @Test void enforcesDurationBounds() {
        rejects(wav(0.99, true), ErrorCode.SPEAKING_AUDIO_TOO_SHORT);
        rejects(wav(180.01, true), ErrorCode.SPEAKING_AUDIO_TOO_LONG);
    }

    @Test void rejectsIncorrectRateAndCompressedCodec() {
        byte[] wrongRate = wav(1, true);
        ByteBuffer.wrap(wrongRate).order(ByteOrder.LITTLE_ENDIAN).putInt(24, 44100);
        rejects(wrongRate, ErrorCode.SPEAKING_AUDIO_FORMAT);
        byte[] compressed = wav(1, true);
        ByteBuffer.wrap(compressed).order(ByteOrder.LITTLE_ENDIAN).putShort(20, (short) 3);
        rejects(compressed, ErrorCode.SPEAKING_AUDIO_FORMAT);
    }

    @Test void rejectsTruncatedAndOverflowingChunks() {
        byte[] truncated = java.util.Arrays.copyOf(wav(1, true), 100);
        rejects(truncated, ErrorCode.AUDIO_EMPTY_OR_CORRUPT);
        byte[] overflow = wav(1, true);
        ByteBuffer.wrap(overflow).order(ByteOrder.LITTLE_ENDIAN).putInt(40, -1);
        rejects(overflow, ErrorCode.AUDIO_EMPTY_OR_CORRUPT);
    }

    @Test void acceptsRecorderMetadataChunksWithPadding() {
        byte[] source = wav(1, true);
        var buffer = ByteBuffer.allocate(source.length + 10).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(source, 0, 36).put("JUNK".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(1).put((byte) 0).put((byte) 0).put(source, 36, source.length - 36);
        buffer.putInt(4, buffer.capacity() - 8);
        assertThatCode(() -> validator.validate(buffer.array())).doesNotThrowAnyException();
    }
}
