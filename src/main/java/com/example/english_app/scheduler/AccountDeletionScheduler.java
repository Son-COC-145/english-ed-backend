package com.example.english_app.scheduler;

import com.example.english_app.entity.user.AccountDeletionRequest;
import com.example.english_app.service.user.AccountDataPurgeService;
import com.example.english_app.service.user.AccountDeletionJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionScheduler {

    private final AccountDeletionJobService jobService;
    private final AccountDataPurgeService purgeService;

    @Value("${account.deletion.max-jobs-per-poll:10}")
    private int maxJobsPerPoll;

    @Scheduled(fixedDelayString = "${account.deletion.poll-ms:5000}")
    public void purgeScheduledAccounts() {
        for (int index = 0; index < maxJobsPerPoll; index++) {
            List<AccountDeletionRequest> claimed = jobService.claimBatch(1);
            if (claimed.isEmpty()) {
                return;
            }

            AccountDeletionRequest request = claimed.getFirst();
            try {
                purgeService.purge(request.getUserId());
                jobService.markCompleted(request);
            } catch (Exception exception) {
                log.error("Account deletion failed: requestId={}, userId={}, attempt={}",
                        request.getId(), request.getUserId(), request.getAttemptCount(), exception);
                jobService.markFailed(request, exception);
            }
        }
    }
}
