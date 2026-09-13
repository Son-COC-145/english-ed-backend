package com.example.english_app.service.storage;

import com.example.english_app.repository.storage.StoredFileRepository.StoredFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class StorageLifecycleService {
    private final StoredFileStore store;

    public StoredFile begin(String provider,String key,String type,byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("File must contain between 1 byte and 20 MB");
        }
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            return store.register(provider,key,type,hash,bytes.length);
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public void uploaded(StoredFile file,String url) {
        store.uploaded(file.id(),url);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) store.activate(file.id());
                    else if (status == STATUS_ROLLED_BACK) store.rollbackUpload(file.id());
                }
            });
        }
        // Without a business transaction, keep PENDING until reconciliation confirms a DB reference.
    }
}
