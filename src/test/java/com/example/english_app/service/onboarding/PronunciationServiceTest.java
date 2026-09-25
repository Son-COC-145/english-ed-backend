package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementPronunciationAnswerResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.audio.AudioAssessmentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PronunciationServiceTest {

    @Mock private AudioAssessmentPort audioAssessmentPort;
    @Mock private PlacementTestService placementTestService;
    @Mock private PronunciationSubmissionClaimService claimService;

    @InjectMocks
    private PronunciationService pronunciationService;

    private MockMultipartFile audioFile;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        audioFile = new MockMultipartFile(
                "audioFile", "test.wav", "audio/wav", "dummy_audio".getBytes());
        submissionId = UUID.randomUUID();
    }

    @Test
    void submitPronunciation_usesServerReferenceAndPersistsAssessment() {
        PronunciationScoreResult score = PronunciationScoreResult.builder()
                .word("hello").overallScore((short) 65).status("SCORED").build();
        PlacementPronunciationAnswerResponse response = PlacementPronunciationAnswerResponse.builder()
                .sessionId(100L).submittedQuestionId(200L).pronunciationResult(score).build();

        when(placementTestService.preparePronunciationSubmission(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString()))
                .thenReturn(new PlacementTestService.PronunciationSubmissionPreparation("hello", null));
        when(audioAssessmentPort.assess(any(byte[].class), eq("hello"))).thenReturn(score);
        when(placementTestService.recordPronunciationAssessment(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString(), eq(score)))
                .thenReturn(response);

        PronunciationScoreResult result = pronunciationService.submitPronunciationWithProgression(
                1L, 100L, 200L, submissionId, audioFile).getPronunciationResult();

        assertThat(result).isSameAs(score);
        verify(audioAssessmentPort).assess(any(byte[].class), eq("hello"));
        verify(placementTestService).recordPronunciationAssessment(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString(), eq(score));
        verify(claimService).claim(eq(100L), eq(200L), eq(submissionId), anyString());
        verify(claimService).complete(eq(100L), eq(submissionId), anyString());
    }

    @Test
    void submitPronunciation_forwardsLowScoreWithoutChangingIt() {
        PronunciationScoreResult score = PronunciationScoreResult.builder()
                .word("hello").overallScore((short) 59).status("SCORED").build();
        when(placementTestService.preparePronunciationSubmission(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString()))
                .thenReturn(new PlacementTestService.PronunciationSubmissionPreparation("hello", null));
        when(audioAssessmentPort.assess(any(byte[].class), eq("hello"))).thenReturn(score);
        when(placementTestService.recordPronunciationAssessment(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString(), eq(score)))
                .thenReturn(PlacementPronunciationAnswerResponse.builder()
                        .pronunciationResult(score).build());

        PronunciationScoreResult result = pronunciationService.submitPronunciationWithProgression(
                1L, 100L, 200L, submissionId, audioFile).getPronunciationResult();

        assertThat(result.getOverallScore()).isEqualTo((short) 59);
    }

    @Test
    void submitPronunciation_replaysProcessedSubmissionWithoutCallingAzure() {
        PlacementPronunciationAnswerResponse replay = PlacementPronunciationAnswerResponse.builder()
                .sessionId(100L).submittedQuestionId(200L).build();
        when(placementTestService.preparePronunciationSubmission(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString()))
                .thenReturn(new PlacementTestService.PronunciationSubmissionPreparation("hello", replay));

        PlacementPronunciationAnswerResponse result = pronunciationService.submitPronunciationWithProgression(
                1L, 100L, 200L, submissionId, audioFile);

        assertThat(result).isSameAs(replay);
        verifyNoInteractions(audioAssessmentPort);
        verifyNoInteractions(claimService);
        verify(placementTestService, never()).recordPronunciationAssessment(
                anyLong(), anyLong(), anyLong(), any(), anyString(), any());
    }

    @Test
    void submitPronunciation_doesNotCallAzureWhenPreflightRejectsDuplicate() {
        when(placementTestService.preparePronunciationSubmission(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString()))
                .thenThrow(ErrorCode.ANSWER_ALREADY_SUBMITTED.toException());

        assertThatThrownBy(() -> pronunciationService.submitPronunciationWithProgression(
                1L, 100L, 200L, submissionId, audioFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ANSWER_ALREADY_SUBMITTED));

        verifyNoInteractions(audioAssessmentPort);
        verifyNoInteractions(claimService);
    }

    @Test
    void unavailableProviderDoesNotPersistOrAdvancePlacement() {
        PronunciationScoreResult unavailable = PronunciationScoreResult.builder()
                .word("hello").status("UNAVAILABLE").build();
        when(placementTestService.preparePronunciationSubmission(
                eq(1L), eq(100L), eq(200L), eq(submissionId), anyString()))
                .thenReturn(new PlacementTestService.PronunciationSubmissionPreparation("hello", null));
        when(audioAssessmentPort.assess(any(byte[].class), eq("hello"))).thenReturn(unavailable);

        assertThatThrownBy(() -> pronunciationService.submitPronunciationWithProgression(
                1L, 100L, 200L, submissionId, audioFile))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PRONUNCIATION_UNAVAILABLE));

        verify(placementTestService, never()).recordPronunciationAssessment(
                anyLong(), anyLong(), anyLong(), any(), anyString(), any());
        verify(claimService).release(eq(100L), eq(submissionId), anyString());
    }
}
