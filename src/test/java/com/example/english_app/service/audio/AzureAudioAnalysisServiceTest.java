package com.example.english_app.service.audio;

import com.example.english_app.config.AzureSpeechConfig;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureAudioAnalysisServiceTest {

    @Mock
    private AzureSpeechConfig azureSpeechConfig;

    @InjectMocks
    private AzureAudioAnalysisService audioAnalysisService;

    @BeforeEach
    void setUp() {
        lenient().when(azureSpeechConfig.region()).thenReturn("southeastasia");
        lenient().when(azureSpeechConfig.subscriptionKey()).thenReturn("dummy_key");
        lenient().when(azureSpeechConfig.timeoutSeconds()).thenReturn(5);
        ReflectionTestUtils.setField(audioAnalysisService, "azureSpeechConfig", azureSpeechConfig);
    }

    @Test
    void assess_ShouldThrowException_WhenAudioIsEmpty() {
        AppException exception = assertThrows(AppException.class, () -> {
            audioAnalysisService.assess(new byte[0], "hello");
        });
        assertEquals(5010, exception.getErrorCode().getCode()); // AUDIO_PROCESSING_FAILED
    }

    @Test
    void assess_ShouldThrowException_WhenMagicBytesInvalid() {
        byte[] invalidAudio = "This is a fake audio file".getBytes();
        
        AppException exception = assertThrows(AppException.class, () -> {
            audioAnalysisService.assess(invalidAudio, "hello");
        });
        assertEquals(5010, exception.getErrorCode().getCode());
    }
}
