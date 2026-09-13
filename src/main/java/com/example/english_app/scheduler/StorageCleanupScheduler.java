package com.example.english_app.scheduler;

import com.example.english_app.service.storage.StoredFileStore;
import com.example.english_app.service.storage.AzureBlobStorageService;
import com.example.english_app.service.integration.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageCleanupScheduler {
    private final StoredFileStore store;
    private final CloudinaryService cloudinary;
    private final AzureBlobStorageService azure;

    @Scheduled(fixedDelayString = "${storage.cleanup.poll-ms:30000}")
    public void cleanup() {
        for (var file : store.claimCleanup()) {
            try {
                if (store.hasReferences(file.url())) {
                    store.finish(file,"ACTIVE",null);
                    continue;
                }
                switch (file.provider()) {
                    case "CLOUDINARY" -> cloudinary.deleteFile(file.objectKey(),file.resourceType());
                    case "AZURE" -> azure.deleteBlob(file.objectKey());
                    default -> throw new IllegalStateException("Unsupported storage provider");
                }
                store.finish(file,"DELETED",null);
            } catch (RuntimeException exception) {
                store.finish(file,file.attempts() >= 9 ? "FAILED" : "DELETE_PENDING",exception.getClass().getSimpleName());
                log.warn("Storage cleanup failed: fileId={} provider={}",file.id(),file.provider());
            }
        }
    }

    @Scheduled(fixedDelayString = "${storage.cleanup.reconcile-ms:3600000}")
    public void reconcile() { store.reconcile(); }
}
