package com.example.english_app.service.storage;

import com.example.english_app.repository.storage.StoredFileRepository;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class StoredFileStoreTest {
    private final StoredFileRepository repository = mock(StoredFileRepository.class);
    private final StoredFileStore store = new StoredFileStore(repository);

    @Test void knownLegacyMaterialGetsDurableDeletionJob() {
        store.queueMaterialDeletion(1L,"https://file","materials/key","raw");
        verify(repository).recordLegacy("materials/key","raw","https://file","DELETE_PENDING",null);
        verify(repository).queueDeletionByUrl("https://file");
    }

    @Test void missingMetadataIsRecordedForManualReview() {
        store.queueMaterialDeletion(1L,"https://file",null,null);
        verify(repository).recordLegacy(eq("legacy-material:1"),eq("unknown"),eq("https://file"),
                eq("LEGACY_REVIEW"),anyString());
    }
}
