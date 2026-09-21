package com.example.english_app.service.storage;

import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;
import com.example.english_app.scheduler.StorageCleanupScheduler;
import com.example.english_app.service.integration.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;

class StorageCleanupSchedulerTest {
    private final StoredFileStore store = mock(StoredFileStore.class);
    private final CloudinaryService cloud = mock(CloudinaryService.class);
    private final AzureBlobStorageService azure = mock(AzureBlobStorageService.class);
    private final StorageCleanupScheduler scheduler = new StorageCleanupScheduler(store,cloud,azure);
    private final StoredFile file = new StoredFile(1L,"CLOUDINARY","key","raw","hash","https://file","PROCESSING",0,"claim");

    private void prepare(StoredFile job) {
        ReflectionTestUtils.setField(scheduler,"maxJobsPerPoll",25);
        when(store.claimCleanup()).thenReturn(List.of(job),List.of());
    }
    @Test void cloudFailureSchedulesRetry() {
        prepare(file);
        doThrow(new IllegalStateException()).when(cloud).deleteFile("key","raw");
        scheduler.cleanup();
        verify(store).finish(file,"DELETE_PENDING","IllegalStateException");
        verify(store,never()).finish(file,"DELETED",null);
    }
    @Test void successfulRetryCompletesDeletion() {
        prepare(file);
        scheduler.cleanup();
        verify(cloud).deleteFile("key","raw");
        verify(store).finish(file,"DELETED",null);
    }
    @Test void sharedMaterialIsNotDeleted() {
        prepare(file);
        when(store.hasReferences("https://file")).thenReturn(true);
        scheduler.cleanup();
        verifyNoInteractions(cloud,azure);
        verify(store).finish(file,"ACTIVE",null);
    }
    @Test void tenthFailureStopsAutomaticRetry() {
        StoredFile exhausted = new StoredFile(1L,"CLOUDINARY","key","raw","hash","https://file","PROCESSING",9,"claim");
        prepare(exhausted);
        doThrow(new IllegalStateException()).when(cloud).deleteFile("key","raw");
        scheduler.cleanup();
        verify(store).finish(exhausted,"FAILED","IllegalStateException");
    }
}
