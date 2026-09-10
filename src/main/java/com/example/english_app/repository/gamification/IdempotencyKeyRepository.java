package com.example.english_app.repository.gamification;

import com.example.english_app.entity.gamification.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndAttemptId(Long userId, String attemptId);

    /** Dùng cho cleanup scheduler – xóa các key đã hết hạn (> 24h) */
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
