package com.example.english_app.repository.gamification;

import com.example.english_app.entity.gamification.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndOperationTypeAndAttemptId(
            Long userId, String operationType, String attemptId);

    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (user_id, operation_type, attempt_id, request_hash, result_json, created_at) "
            + "VALUES (:userId, :operationType, :attemptId, :requestHash, NULL, NOW()) "
            + "ON CONFLICT (user_id, operation_type, attempt_id) DO NOTHING", nativeQuery = true)
    int claim(@Param("userId") Long userId,
              @Param("operationType") String operationType,
              @Param("attemptId") String attemptId,
              @Param("requestHash") String requestHash);

    /** Dùng cho cleanup scheduler – xóa các key đã hết hạn (> 24h) */
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
