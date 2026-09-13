package com.example.english_app.service.storage;

import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageLifecycleServiceTest {
    private final StoredFileStore store = mock(StoredFileStore.class);
    private final StorageLifecycleService service = new StorageLifecycleService(store);
    private final StoredFile file = new StoredFile(1L,"CLOUDINARY","key","raw","hash",null,"PENDING",0,null);

    @AfterEach void clear() { TransactionSynchronizationManager.clear(); }

    @Test void rollbackQueuesDurableCleanup() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        service.uploaded(file,"https://file");
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(store).uploaded(1L,"https://file");
        verify(store).rollbackUpload(1L);
        verify(store,never()).activate(any());
    }

    @Test void commitActivatesUpload() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        service.uploaded(file,"https://file");
        TransactionSynchronizationManager.getSynchronizations().forEach(callback ->
                callback.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(store).activate(1L);
        verify(store,never()).rollbackUpload(any());
    }

    @Test void standaloneUploadIsRetainedForLaterAttachment() {
        service.uploaded(file,"https://file");
        verify(store).uploaded(1L,"https://file");
        verify(store).activate(1L);
    }

    @Test void rejectsEmptyUploadBeforeDatabase() {
        assertThrows(IllegalArgumentException.class, () -> service.begin("CLOUDINARY","key","raw",new byte[0]));
        verifyNoInteractions(store);
    }
}
