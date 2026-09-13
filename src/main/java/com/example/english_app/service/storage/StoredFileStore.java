package com.example.english_app.service.storage;

import com.example.english_app.repository.storage.StoredFileRepository;
import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoredFileStore {
    private final StoredFileRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public StoredFile register(String provider,String key,String type,String hash,long byteSize) {
        return repository.register(provider,key,type,hash,byteSize);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void uploaded(Long id,String url) { repository.uploaded(id,url); }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(Long id) { repository.activate(id); }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rollbackUpload(Long id) { repository.queueDeletion(id); }
    @Transactional
    public void queueDeletionByUrl(String url) { repository.queueDeletionByUrl(url); }
    @Transactional
    public List<StoredFile> claimCleanup() { return repository.claimCleanup(25); }
    @Transactional
    public void finish(StoredFile file,String status,String error) { repository.completeCleanup(file,status,error); }
    @Transactional(readOnly = true)
    public boolean hasReferences(String url) { return repository.hasReferences(url); }
    @Transactional
    public void reconcile() {
        for (StoredFile file : repository.findReconciliationCandidates(25)) {
            if (repository.hasReferences(file.url())) {
                repository.activate(file.id());
                repository.touch(file.id());
            } else repository.queueDeletion(file.id());
        }
    }
}
