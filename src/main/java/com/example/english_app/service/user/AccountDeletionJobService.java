package com.example.english_app.service.user;

import com.example.english_app.entity.enums.AccountDeletionStatus;
import com.example.english_app.entity.user.AccountDeletionRequest;
import com.example.english_app.repository.user.AccountDeletionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDeletionJobService {

    private static final int MAX_ATTEMPTS = 5;

    private final AccountDeletionRequestRepository repository;

    @Transactional
    public List<AccountDeletionRequest> claimBatch(int limit) {
        List<AccountDeletionRequest> requests = repository.lockDispatchable(limit);
        LocalDateTime now = LocalDateTime.now();
        for (AccountDeletionRequest request : requests) {
            request.setStatus(AccountDeletionStatus.PROCESSING);
            request.setAttemptCount(request.getAttemptCount() + 1);
            request.setLockedAt(now);
            request.setClaimToken(UUID.randomUUID().toString());
            request.setLastError(null);
        }
        return requests;
    }

    @Transactional
    public void markCompleted(AccountDeletionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        repository.completeClaim(
                request.getId(),
                request.getClaimToken(),
                AccountDeletionStatus.COMPLETED,
                null,
                now,
                now);
    }

    @Transactional
    public void markFailed(AccountDeletionRequest request, Exception exception) {
        int attempts = request.getAttemptCount();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        long retryDelaySeconds = Math.min(3600, 1L << Math.min(attempts, 10));
        String error = exception.getClass().getSimpleName();
        repository.completeClaim(
                request.getId(),
                request.getClaimToken(),
                terminal ? AccountDeletionStatus.FAILED : AccountDeletionStatus.PENDING,
                error,
                LocalDateTime.now().plusSeconds(retryDelaySeconds),
                null);
    }
}
