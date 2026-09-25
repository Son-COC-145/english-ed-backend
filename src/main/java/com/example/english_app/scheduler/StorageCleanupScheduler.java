package com.example.english_app.scheduler;

import com.example.english_app.service.storage.StoredFileStore;
import com.example.english_app.service.storage.AzureBlobStorageService;
import com.example.english_app.service.integration.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupScheduler {
    private final StoredFileStore store;
    private final CloudinaryService cloudinary;
    private final AzureBlobStorageService azure;
    @Value("${storage.cleanup.max-jobs-per-poll:25}")
    private int maxJobsPerPoll;

    // Chạy lúc 2h sáng mỗi ngày
    @Scheduled(cron = "${storage.cleanup.cron:0 0 2 * * *}")
    public void cleanup() {
        for (int index = 0; index < maxJobsPerPoll; index++) {
            List<StoredFile> claimed = store.claimCleanup();
            if (claimed.isEmpty())
                break;
            StoredFile file = claimed.getFirst();
            try {
                if (store.hasReferences(file.url())) {
                    store.finish(file, "ACTIVE", null);
                    continue;
                }
                switch (file.provider()) {
                    case "CLOUDINARY" -> cloudinary.deleteFile(file.objectKey(), file.resourceType());
                    case "AZURE" -> azure.deleteBlob(file.objectKey());
                    default -> throw new IllegalStateException("Unsupported storage provider");
                }
                store.finish(file, "DELETED", null);
            } catch (RuntimeException exception) {
                store.finish(file, file.attempts() >= 9 ? "FAILED" : "DELETE_PENDING",
                        exception.getClass().getSimpleName());
                log.warn("Storage cleanup failed: fileId={} provider={}", file.id(), file.provider());
            }
        }
    }

}
