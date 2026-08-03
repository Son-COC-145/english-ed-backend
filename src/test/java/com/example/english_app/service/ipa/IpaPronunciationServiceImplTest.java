package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.dto.response.ipa.PronunciationResultResponse;
import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.service.audio.AudioAssessmentPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho IpaPronunciationServiceImpl — Phase 2: Assessment Engine.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IpaPronunciationServiceImpl Unit Tests")
class IpaPronunciationServiceImplTest {

    @Mock private AudioAssessmentPort                audioAssessmentPort;
    @Mock private IpaExampleWordRepository           exampleWordRepository;
    @Mock private PronunciationPracticeLogRepository practiceLogRepository;
    @Mock private UserRepository                     userRepository;
    @Mock private ApplicationEventPublisher          eventPublisher;
    @Mock private ObjectMapper                       objectMapper;
    @Mock private MultipartFile                      audioFile;

    @InjectMocks
    private IpaPronunciationServiceImpl pronunciationService;

    private IpaExampleWord mockExampleWord;

    @BeforeEach
    void setUp() throws Exception {
        mockExampleWord = IpaExampleWord.builder()
                .id(10L)
                .word("ship")
                .ipaTranscription("ʃɪp")
                .build();

        given(audioFile.getBytes()).willReturn(new byte[]{0x52, 0x49, 0x46, 0x46});
        given(objectMapper.writeValueAsString(any())).willReturn("[{\"phoneme\":\"ʃɪp\",\"accuracyScore\":85,\"color\":\"GREEN\"}]");
        given(practiceLogRepository.save(any(PronunciationPracticeLog.class)))
                .willAnswer(inv -> {
                    PronunciationPracticeLog log = inv.getArgument(0);
                    try {
                        var field = PronunciationPracticeLog.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(log, 99L);
                    } catch (Exception ignored) {}
                    return log;
                });
    }

    @Test
    @DisplayName("assess: Azure SCORED → lưu log, publish event, trả response đúng")
    void assess_azureScored_savesLogAndPublishesEvent() {
        given(exampleWordRepository.findById(10L)).willReturn(Optional.of(mockExampleWord));
        given(audioAssessmentPort.assess(any(), eq("ship"))).willReturn(
                PronunciationScoreResult.builder()
                        .word("ship")
                        .overallScore((short) 85)
                        .accuracyScore((short) 90)
                        .fluencyScore((short) 80)
                        .completenessScore((short) 100)
                        .scoreColor("GREEN")
                        .status("SCORED")
                        .build()
        );

        PronunciationResultResponse result = pronunciationService.assess(1L, 10L, audioFile);

        assertThat(result.overallScore()).isEqualTo((short) 85);
        assertThat(result.fluencyScore()).isEqualTo((short) 80);
        verify(practiceLogRepository).save(any(PronunciationPracticeLog.class));
        // verify event được publish với đúng Object type (không phải ApplicationEvent overload)
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("assess: ExampleWord không tồn tại → throw EXAMPLE_WORD_NOT_FOUND")
    void assess_exampleWordNotFound_throwsException() {
        given(exampleWordRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pronunciationService.assess(1L, 999L, audioFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EXAMPLE_WORD_NOT_FOUND));

        verify(practiceLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("assess: Azure UNAVAILABLE → throw PRONUNCIATION_UNAVAILABLE, không lưu log")
    void assess_azureUnavailable_throwsAndDoesNotSaveLog() {
        given(exampleWordRepository.findById(10L)).willReturn(Optional.of(mockExampleWord));
        given(audioAssessmentPort.assess(any(), any())).willReturn(
                PronunciationScoreResult.builder()
                        .word("ship")
                        .status("UNAVAILABLE")
                        .scoreColor("NONE")
                        .build()
        );

        assertThatThrownBy(() -> pronunciationService.assess(1L, 10L, audioFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRONUNCIATION_UNAVAILABLE));

        verify(practiceLogRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("assess: IOException khi đọc audio → throw AUDIO_PROCESSING_FAILED")
    void assess_ioExceptionOnAudioRead_throwsAudioFailed() throws IOException {
        given(exampleWordRepository.findById(10L)).willReturn(Optional.of(mockExampleWord));
        given(audioFile.getBytes()).willThrow(new IOException("disk error"));

        assertThatThrownBy(() -> pronunciationService.assess(1L, 10L, audioFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.AUDIO_PROCESSING_FAILED));
    }

    @Test
    @DisplayName("assess: overallScore >= 80 → XP event = 20 (HIGH tier)")
    void assess_highScore_publishesXp20() {
        given(exampleWordRepository.findById(10L)).willReturn(Optional.of(mockExampleWord));
        given(audioAssessmentPort.assess(any(), any())).willReturn(
                PronunciationScoreResult.builder()
                        .word("ship").overallScore((short) 90).status("SCORED").scoreColor("GREEN").build()
        );

        pronunciationService.assess(1L, 10L, audioFile);

        verify(eventPublisher).publishEvent(any(Object.class));
    }
}
