package com.example.english_app.service.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.integration.CloudinaryService;

import lombok.RequiredArgsConstructor;

/** Stores short teacher audio comments (uploaded files or browser recordings) and returns their HTTPS URL. */
@Service
@RequiredArgsConstructor
public class AudioUploadService {
    static final long MAX_AUDIO_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> MP3_TYPES = Set.of("audio/mpeg", "audio/mp3");
    private static final Set<String> WAV_TYPES = Set.of("audio/wav", "audio/x-wav", "audio/wave");
    private static final Set<String> MP4_TYPES = Set.of("audio/mp4", "audio/x-m4a");
    private static final String WEBM_TYPE = "audio/webm";

    private final CloudinaryService cloudinaryService;
    private final FileContentValidator contentValidator;

    public String uploadAudio(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw ErrorCode.INVALID_AUDIO_FILE.toException();
        }
        String mime = baseMimeType(file.getContentType());
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INVALID_AUDIO_FILE, "Không thể đọc file âm thanh tải lên");
        }
        if (!hasValidSignature(file, mime, bytes)) {
            throw ErrorCode.INVALID_AUDIO_FILE.toException();
        }
        // Cloudinary serves audio through the "video" resource type.
        return cloudinaryService.uploadFile(bytes, "video", "english-app/audio-comments/" + UUID.randomUUID());
    }

    private boolean hasValidSignature(MultipartFile file, String mime, byte[] bytes) {
        if (MP3_TYPES.contains(mime)) {
            try {
                contentValidator.validate(file);
                return true;
            } catch (AppException ex) {
                return false;
            }
        }
        if (WAV_TYPES.contains(mime)) {
            return bytes.length >= 12 && ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WAVE");
        }
        if (MP4_TYPES.contains(mime)) {
            return bytes.length >= 12 && ascii(bytes, 4, 4).equals("ftyp");
        }
        if (WEBM_TYPE.equals(mime)) {
            return bytes.length >= 4 && (bytes[0] & 255) == 0x1A && (bytes[1] & 255) == 0x45
                    && (bytes[2] & 255) == 0xDF && (bytes[3] & 255) == 0xA3;
        }
        return false;
    }

    /** Browser recordings send parameters such as "audio/webm;codecs=opus". */
    private static String baseMimeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int separator = contentType.indexOf(';');
        return (separator >= 0 ? contentType.substring(0, separator) : contentType).trim().toLowerCase(Locale.ROOT);
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }
}
