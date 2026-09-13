package com.example.english_app.repository.storage;

import com.example.english_app.entity.storage.StoredFileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

/** All methods are called within transactions provided by StoredFileStore. */
public interface StoredFileRepository extends JpaRepository<StoredFileRecord, Long> {
    record StoredFile(Long id, String provider, String objectKey, String resourceType,
                      String contentHash, String url, String status, int attempts, String claimToken) {}

    @Modifying
    @Query(value = "INSERT INTO stored_files(provider,object_key,resource_type,content_hash,byte_size) " +
            "VALUES (:provider,:key,:type,:hash,:size) ON CONFLICT(provider,object_key,resource_type) DO NOTHING", nativeQuery = true)
    void insertUpload(@Param("provider") String provider,@Param("key") String key,@Param("type") String type,
                      @Param("hash") String hash,@Param("size") long size);

    @Query(value = "SELECT * FROM stored_files WHERE provider=:provider AND object_key=:key " +
            "AND resource_type=:type FOR UPDATE", nativeQuery = true)
    StoredFileRecord lockObject(@Param("provider") String provider,@Param("key") String key,@Param("type") String type);

    default StoredFile snapshot(StoredFileRecord file) {
        return new StoredFile(file.getId(),file.getProvider(),file.getObjectKey(),file.getResourceType(),
                file.getContentHash(),file.getUrl(),file.getStatus(),file.getAttempts(),file.getClaimToken());
    }

    default StoredFile register(String provider,String key,String type,String hash,long size) {
        insertUpload(provider,key,type,hash,size);
        StoredFileRecord file = lockObject(provider,key,type);
        if (file == null || !file.getContentHash().equals(hash)
                || !List.of("ACTIVE","PENDING").contains(file.getStatus())) {
            throw new IllegalStateException("Storage object key is already in use; use a new key");
        }
        return snapshot(file);
    }

    @Modifying
    @Query(value = "UPDATE stored_files SET url=:url,updated_at=NOW() WHERE id=:id", nativeQuery = true)
    void uploaded(@Param("id") Long id,@Param("url") String url);

    @Modifying
    @Query(value = "UPDATE stored_files SET status='ACTIVE',updated_at=NOW() WHERE id=:id AND status='PENDING'", nativeQuery = true)
    void activate(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE stored_files SET status='DELETE_PENDING',available_at=NOW(),updated_at=NOW() " +
            "WHERE id=:id AND status IN ('ACTIVE','PENDING','FAILED')", nativeQuery = true)
    void queueDeletion(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE stored_files SET status='DELETE_PENDING',available_at=NOW(),updated_at=NOW() " +
            "WHERE url=:url AND status IN ('ACTIVE','PENDING','FAILED')", nativeQuery = true)
    void queueDeletionByUrl(@Param("url") String url);

    @Query(value = "SELECT * FROM stored_files WHERE (status='DELETE_PENDING' AND available_at<=NOW()) OR " +
            "(status='PROCESSING' AND locked_at<NOW()-INTERVAL '10 minutes') " +
            "ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<StoredFileRecord> lockCleanup(@Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE stored_files SET status='PROCESSING',claim_token=:token,locked_at=NOW() WHERE id=:id", nativeQuery = true)
    void claim(@Param("id") Long id,@Param("token") String token);

    default List<StoredFile> claimCleanup(int limit) {
        return lockCleanup(limit).stream().map(file -> {
            String token = UUID.randomUUID().toString();
            claim(file.getId(),token);
            return new StoredFile(file.getId(),file.getProvider(),file.getObjectKey(),file.getResourceType(),
                    file.getContentHash(),file.getUrl(),"PROCESSING",file.getAttempts(),token);
        }).toList();
    }

    @Modifying
    @Query(value = "UPDATE stored_files SET status=:status,attempt_count=:attempts,last_error=:error," +
            "claim_token=NULL,locked_at=NULL,available_at=NOW()+(:backoff*INTERVAL '1 second'),updated_at=NOW() " +
            "WHERE id=:id AND claim_token=:token AND status='PROCESSING'", nativeQuery = true)
    void complete(@Param("id") Long id,@Param("token") String token,@Param("status") String status,
                  @Param("attempts") int attempts,@Param("error") String error,@Param("backoff") long backoff);

    default void completeCleanup(StoredFile file,String status,String error) {
        int attempts = error == null ? file.attempts() : file.attempts()+1;
        complete(file.id(),file.claimToken(),status,attempts,error,Math.min(3600,1L << Math.min(attempts,10)));
    }

    @Query(value = "SELECT * FROM stored_files WHERE status IN ('PENDING','ACTIVE') " +
            "AND updated_at<NOW()-INTERVAL '48 hours' ORDER BY updated_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<StoredFileRecord> lockReconciliation(@Param("limit") int limit);

    default List<StoredFile> findReconciliationCandidates(int limit) {
        return lockReconciliation(limit).stream().map(this::snapshot).toList();
    }

    @Query(value = "SELECT storage_url_is_referenced(:url)", nativeQuery = true)
    boolean hasReferences(@Param("url") String url);

    @Modifying
    @Query(value = "UPDATE stored_files SET updated_at=NOW() WHERE id=:id", nativeQuery = true)
    void touch(@Param("id") Long id);
}
