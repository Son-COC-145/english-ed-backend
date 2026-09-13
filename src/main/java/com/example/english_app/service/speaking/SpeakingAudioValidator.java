package com.example.english_app.service.speaking;

import com.example.english_app.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** The Speaking recorder contract is uncompressed PCM16 mono 16 kHz WAV. */
@Component
public class SpeakingAudioValidator {
    public void validate(byte[] audio) {
        if (audio.length < 44)
            throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
        if (!tag(audio, 0, "RIFF") || !tag(audio, 8, "WAVE")) {
            throw ErrorCode.SPEAKING_AUDIO_FORMAT.toException();
        }
        
        ByteBuffer bytes = ByteBuffer.wrap(audio).order(ByteOrder.LITTLE_ENDIAN);
        long declaredEnd = Integer.toUnsignedLong(bytes.getInt(4)) + 8;
        
        if (declaredEnd != audio.length)
            throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
        
        boolean format = false;
        int dataOffset = -1, dataSize = 0;
        
        for (int offset = 12; offset < audio.length;) {
            
            if (audio.length - offset < 8)
                throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
            
            long size = Integer.toUnsignedLong(bytes.getInt(offset + 4));
            long end = offset + 8L + size;
            
            if (end > audio.length)
                throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
            
            if (tag(audio, offset, "fmt ")) {
                if (format || size < 16)
                    throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
                
                int at = offset + 8;
                if (bytes.getShort(at) != 1 || bytes.getShort(at + 2) != 1
                        || bytes.getInt(at + 4) != 16000 || bytes.getInt(at + 8) != 32000
                        || bytes.getShort(at + 12) != 2 || bytes.getShort(at + 14) != 16) {
                    throw ErrorCode.SPEAKING_AUDIO_FORMAT.toException();
                }
                format = true;
            } else if (tag(audio, offset, "data")) {
                if (dataOffset != -1)
                    throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
                
                dataOffset = offset + 8;
                dataSize = (int) size;
            }
            long next = end + (size & 1);
            if (next > audio.length)
                throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
            offset = (int) next;
        }
        if (!format || dataOffset < 0 || dataSize == 0 || (dataSize & 1) != 0) {
            throw ErrorCode.AUDIO_EMPTY_OR_CORRUPT.toException();
        }
        double seconds = dataSize / 32000.0;
        if (seconds < 1)
            throw ErrorCode.SPEAKING_AUDIO_TOO_SHORT.toException();
        if (seconds > 180)
            throw ErrorCode.SPEAKING_AUDIO_TOO_LONG.toException();
        // Energy gate, not a linguistic VAD: require at least one 40ms audible window.
        boolean audible = false;
        for (int at = dataOffset; at + 1280 <= dataOffset + dataSize; at += 1280) {
            double energy = 0;
            for (int sample = at; sample < at + 1280; sample += 2) {
                double value = bytes.getShort(sample) / 32768.0;
                energy += value * value;
            }
            if (Math.sqrt(energy / 640) >= 0.012) {
                audible = true;
                break;
            }
        }
        if (!audible)
            throw ErrorCode.SPEAKING_AUDIO_NO_SPEECH.toException();
    }

    private boolean tag(byte[] audio, int offset, String tag) {
        return new String(audio, offset, 4, StandardCharsets.US_ASCII).equals(tag);
    }
}
