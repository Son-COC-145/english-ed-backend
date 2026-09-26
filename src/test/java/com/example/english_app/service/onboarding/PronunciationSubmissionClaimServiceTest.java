package com.example.english_app.service.onboarding;

import com.example.english_app.entity.onboarding.PlacementPronunciationSubmission;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.PlacementPronunciationSubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PronunciationSubmissionClaimServiceTest {

    @Mock private PlacementPronunciationSubmissionRepository repository;
    @InjectMocks private PronunciationSubmissionClaimService service;

    @Test
    void firstRequestOwnsTheProviderCall() {
        UUID id = UUID.randomUUID();
        when(repository.insertClaim(10L, 20L, id, "hash")).thenReturn(1);

        service.claim(10L, 20L, id, "hash");

        verify(repository, never()).findBySessionIdAndSubmissionId(any(), any());
    }

    @Test
    void concurrentDuplicateIsRejectedWhileFirstRequestIsProcessing() {
        UUID id = UUID.randomUUID();
        when(repository.insertClaim(10L, 20L, id, "hash")).thenReturn(0);
        when(repository.reclaimStale(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(repository.findBySessionIdAndSubmissionId(10L, id)).thenReturn(Optional.of(
                claim(10L, 20L, id, "hash")));

        assertThatThrownBy(() -> service.claim(10L, 20L, id, "hash"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.SUBMISSION_IN_PROGRESS));
    }

    @Test
    void sameKeyWithDifferentAudioHashIsRejectedAsConflict() {
        UUID id = UUID.randomUUID();
        when(repository.insertClaim(10L, 20L, id, "new-hash")).thenReturn(0);
        when(repository.reclaimStale(any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(repository.findBySessionIdAndSubmissionId(10L, id)).thenReturn(Optional.of(
                claim(10L, 20L, id, "old-hash")));

        assertThatThrownBy(() -> service.claim(10L, 20L, id, "new-hash"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));
    }

    private PlacementPronunciationSubmission claim(
            Long sessionId, Long questionId, UUID submissionId, String hash) {
        return PlacementPronunciationSubmission.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .submissionId(submissionId)
                .requestHash(hash)
                .status("PROCESSING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
