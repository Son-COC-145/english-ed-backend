package com.example.english_app.service.onboarding;

import com.example.english_app.entity.onboarding.PlacementPronunciationSubmission;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.PlacementPronunciationSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/** Distributed, short-lived claim that prevents duplicate Azure calls across app instances. */
@Service
@RequiredArgsConstructor
public class PronunciationSubmissionClaimService {

    private static final int STALE_AFTER_MINUTES = 2;

    private final PlacementPronunciationSubmissionRepository repository;

    @Transactional
    public void claim(
            Long sessionId,
            Long questionId,
            UUID submissionId,
            String requestHash) {
        if (repository.insertClaim(sessionId, questionId, submissionId, requestHash) == 1) return;

        LocalDateTime now = LocalDateTime.now();
        if (repository.reclaimStale(
                sessionId, questionId, submissionId, requestHash,
                now.minusMinutes(STALE_AFTER_MINUTES), now) == 1) {
            return;
        }

        PlacementPronunciationSubmission existing = repository
                .findBySessionIdAndSubmissionId(sessionId, submissionId)
                .orElseGet(() -> repository.findBySessionIdAndQuestionId(sessionId, questionId)
                        .orElseThrow(() -> ErrorCode.SUBMISSION_IN_PROGRESS.toException()));

        if (existing.getSubmissionId().equals(submissionId)
                && (!existing.getQuestionId().equals(questionId)
                || !existing.getRequestHash().equals(requestHash))) {
            throw ErrorCode.IDEMPOTENCY_KEY_REUSED.toException();
        }
        throw ErrorCode.SUBMISSION_IN_PROGRESS.toException();
    }

    @Transactional
    public void release(Long sessionId, UUID submissionId, String requestHash) {
        repository.release(sessionId, submissionId, requestHash);
    }

    @Transactional
    public void complete(Long sessionId, UUID submissionId, String requestHash) {
        repository.complete(sessionId, submissionId, requestHash, LocalDateTime.now());
    }
}
