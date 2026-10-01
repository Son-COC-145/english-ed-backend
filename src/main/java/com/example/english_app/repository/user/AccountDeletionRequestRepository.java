package com.example.english_app.repository.user;

import com.example.english_app.entity.enums.AccountDeletionStatus;
import com.example.english_app.entity.user.AccountDeletionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountDeletionRequestRepository extends JpaRepository<AccountDeletionRequest, Long> {

    Optional<AccountDeletionRequest> findByUserId(Long userId);

    @Query(value = """
            SELECT * FROM account_deletion_requests
            WHERE (status = 'PENDING' AND available_at <= CURRENT_TIMESTAMP)
               OR (status = 'PROCESSING' AND locked_at < CURRENT_TIMESTAMP - INTERVAL '5 minutes')
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AccountDeletionRequest> lockDispatchable(@Param("limit") int limit);

    @Modifying
    @Query("""
            update AccountDeletionRequest request
               set request.status = :status,
                   request.lockedAt = null,
                   request.claimToken = null,
                   request.lastError = :lastError,
                   request.availableAt = :availableAt,
                   request.completedAt = :completedAt,
                   request.updatedAt = CURRENT_TIMESTAMP
             where request.id = :id
               and request.status = com.example.english_app.entity.enums.AccountDeletionStatus.PROCESSING
               and request.claimToken = :claimToken
            """)
    int completeClaim(
            @Param("id") Long id,
            @Param("claimToken") String claimToken,
            @Param("status") AccountDeletionStatus status,
            @Param("lastError") String lastError,
            @Param("availableAt") LocalDateTime availableAt,
            @Param("completedAt") LocalDateTime completedAt);
}
