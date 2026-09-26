package com.example.english_app.service.storage;

import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.integration.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class AudioUploadServiceTest {
    private static final byte[] WEBM = {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 1, 2, 3, 4};

    private CloudinaryService cloudinaryService;
    private AudioUploadService service;

    @BeforeEach
    void setUp() {
        cloudinaryService = mock(CloudinaryService.class);
        service = new AudioUploadService(cloudinaryService, new FileContentValidator());
    }

    @Test
    void uploadsBrowserRecordingWithCodecParameter() {
        when(cloudinaryService.uploadFile(any(), eq("video"), startsWith("english-app/audio-comments/")))
                .thenReturn("https://cdn.test/comment.webm");

        String url = service.uploadAudio(new MockMultipartFile("file", "comment.webm",
                "audio/webm;codecs=opus", WEBM));

        assertEquals("https://cdn.test/comment.webm", url);
    }

    @Test
    void acceptsMp3WithId3Header() {
        when(cloudinaryService.uploadFile(any(), eq("video"), any())).thenReturn("https://cdn.test/comment.mp3");

        assertEquals("https://cdn.test/comment.mp3", service.uploadAudio(new MockMultipartFile("file",
                "comment.mp3", "audio/mpeg", "ID3tagdata".getBytes())));
    }

    @Test
    void rejectsFileWhoseContentDoesNotMatchMime() {
        AppException error = assertThrows(AppException.class, () -> service.uploadAudio(
                new MockMultipartFile("file", "comment.wav", "audio/wav", new byte[]{77, 90, 0, 0})));

        assertEquals(ErrorCode.INVALID_AUDIO_FILE, error.getErrorCode());
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void rejectsUnsupportedMimeType() {
        AppException error = assertThrows(AppException.class, () -> service.uploadAudio(
                new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes())));

        assertEquals(ErrorCode.INVALID_AUDIO_FILE, error.getErrorCode());
    }

    @Test
    void rejectsFileLargerThanLimit() {
        byte[] large = new byte[(int) AudioUploadService.MAX_AUDIO_SIZE_BYTES + 1];
        System.arraycopy(WEBM, 0, large, 0, WEBM.length);

        AppException error = assertThrows(AppException.class, () -> service.uploadAudio(
                new MockMultipartFile("file", "long.webm", "audio/webm", large)));

        assertEquals(ErrorCode.INVALID_AUDIO_FILE, error.getErrorCode());
        verifyNoInteractions(cloudinaryService);
    }
}
