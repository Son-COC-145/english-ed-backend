package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementPronunciationAnswerResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.service.audio.AudioAssessmentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/** Handles audio assessment while placement state remains owned by PlacementTestService. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationService {

    private final AudioAssessmentPort audioAssessmentPort;
    private final PlacementTestService placementTestService;
    private final PronunciationSubmissionClaimService claimService;

    /**
     * Validate first, assess without a DB transaction, then revalidate and persist under row lock.
     */
    public PlacementPronunciationAnswerResponse submitPronunciationWithProgression(
            Long userId,
            Long sessionId,
            Long questionId,
            UUID submissionId,
            MultipartFile audioFile) {

        byte[] audioBytes = readAudioBytes(audioFile);
        String requestHash = PlacementSubmissionHasher.pronunciation(
                sessionId, questionId, audioBytes);
        PlacementTestService.PronunciationSubmissionPreparation preparation =
                placementTestService.preparePronunciationSubmission(
                        userId, sessionId, questionId, submissionId, requestHash);
        if (preparation.replayResponse() != null) {
            return preparation.replayResponse();
        }
        String referenceText = preparation.referenceText();

        claimService.claim(sessionId, questionId, submissionId, requestHash);
        try {
            // External network call intentionally runs outside a database transaction.
            PronunciationScoreResult result = audioAssessmentPort.assess(audioBytes, referenceText);
            log.info("Pronunciation assessed: userId={}, sessionId={}, questionId={}, status={}, score={}",
                    userId, sessionId, questionId,
                    result != null ? result.getStatus() : "NULL",
                    result != null ? result.getOverallScore() : null);

            // Provider/network failures are not learner mistakes. Keep the current question assigned
            // so Mobile can safely retry without changing the placement state.
            if (result == null
                    || !"SCORED".equals(result.getStatus())
                    || result.getOverallScore() == null) {
                throw ErrorCode.PRONUNCIATION_UNAVAILABLE.toException();
            }

            PlacementPronunciationAnswerResponse response = placementTestService.recordPronunciationAssessment(
                    userId, sessionId, questionId, submissionId, requestHash, result);
            claimService.complete(sessionId, submissionId, requestHash);
            return response;
        } catch (RuntimeException exception) {
            // The claim is coordination metadata, not placement progress. Releasing it lets the
            // same logical submit retry after invalid audio, provider outage, or a transient error.
            claimService.release(sessionId, submissionId, requestHash);
            throw exception;
        }
    }

    private byte[] readAudioBytes(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw ErrorCode.AUDIO_PROCESSING_FAILED.toException();
        }
        try {
            return audioFile.getBytes();
        } catch (IOException e) {
            log.error("Failed to read audio file bytes", e);
            throw ErrorCode.AUDIO_PROCESSING_FAILED.toException();
        }
    }
}
