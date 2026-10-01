package com.example.english_app.service.user;

import com.example.english_app.entity.enums.AccountDeletionStatus;
import com.example.english_app.entity.user.AccountDeletionRequest;
import com.example.english_app.repository.user.AccountDeletionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionJobServiceTest {

    @Mock
    private AccountDeletionRequestRepository repository;

    @Test
    void claimBatch_assignsUniqueClaimAndIncrementsAttempt() {
        AccountDeletionRequest request = requestWithAttempts(0);
        when(repository.lockDispatchable(1)).thenReturn(List.of(request));
        AccountDeletionJobService service = new AccountDeletionJobService(repository);

        List<AccountDeletionRequest> claimed = service.claimBatch(1);

        assertThat(claimed).containsExactly(request);
        assertThat(request.getStatus()).isEqualTo(AccountDeletionStatus.PROCESSING);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getClaimToken()).isNotBlank();
        assertThat(request.getLockedAt()).isNotNull();
    }

    @Test
    void markFailed_requeuesBeforeMaxAttempts() {
        AccountDeletionRequest request = requestWithAttempts(1);
        request.setStatus(AccountDeletionStatus.PROCESSING);
        request.setClaimToken("claim");
        AccountDeletionJobService service = new AccountDeletionJobService(repository);

        service.markFailed(request, new IllegalStateException("provider unavailable"));

        verify(repository).completeClaim(
                eq(7L),
                eq("claim"),
                eq(AccountDeletionStatus.PENDING),
                eq("IllegalStateException"),
                any(LocalDateTime.class),
                eq(null));
    }

    @Test
    void markFailed_stopsAfterMaxAttempts() {
        AccountDeletionRequest request = requestWithAttempts(5);
        request.setStatus(AccountDeletionStatus.PROCESSING);
        request.setClaimToken("claim");
        AccountDeletionJobService service = new AccountDeletionJobService(repository);

        service.markFailed(request, new IllegalStateException("persistent failure"));

        verify(repository).completeClaim(
                eq(7L),
                eq("claim"),
                eq(AccountDeletionStatus.FAILED),
                eq("IllegalStateException"),
                any(LocalDateTime.class),
                eq(null));
    }

    private AccountDeletionRequest requestWithAttempts(int attempts) {
        return AccountDeletionRequest.builder()
                .id(7L)
                .userId(42L)
                .status(AccountDeletionStatus.PENDING)
                .attemptCount(attempts)
                .availableAt(LocalDateTime.now())
                .build();
    }
}
