package com.example.english_app.service.user;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.storage.StoredFileStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDataPurgeService {

    private static final List<String> DELETE_PERSONAL_DATA_SQL = List.of(
            "DELETE FROM notification_push_deliveries WHERE outbox_event_id IN "
                    + "(SELECT id FROM notification_outbox WHERE recipient_id = ?)",
            "DELETE FROM notifications WHERE user_id = ?",
            "DELETE FROM notification_outbox WHERE recipient_id = ?",
            "DELETE FROM user_device_tokens WHERE user_id = ?",

            "DELETE FROM assignment_submissions WHERE student_id = ?",
            "DELETE FROM class_students WHERE student_id = ?",

            "DELETE FROM speaking_job_attempts WHERE job_id IN "
                    + "(SELECT id FROM speaking_jobs WHERE session_id IN "
                    + "(SELECT id FROM speaking_sessions WHERE student_id = ?))",
            "DELETE FROM speaking_jobs WHERE session_id IN "
                    + "(SELECT id FROM speaking_sessions WHERE student_id = ?)",
            "DELETE FROM speaking_reward_ledger WHERE student_id = ?",
            "DELETE FROM speaking_start_requests WHERE student_id = ?",
            "DELETE FROM speaking_turns WHERE session_id IN "
                    + "(SELECT id FROM speaking_sessions WHERE student_id = ?)",
            "DELETE FROM speaking_sessions WHERE student_id = ?",

            "DELETE FROM placement_pronunciation_submissions WHERE session_id IN "
                    + "(SELECT id FROM placement_test_sessions WHERE student_id = ?)",
            "DELETE FROM placement_test_answers WHERE session_id IN "
                    + "(SELECT id FROM placement_test_sessions WHERE student_id = ?)",
            "DELETE FROM placement_test_sessions WHERE student_id = ?",
            "DELETE FROM roadmap_generation_jobs WHERE student_id = ?",
            "DELETE FROM roadmap_module_progress WHERE student_id = ?",
            "DELETE FROM student_onboarding WHERE student_id = ?",

            "DELETE FROM minigame_results WHERE student_id = ?",
            "DELETE FROM minigame_rounds WHERE student_id = ?",
            "DELETE FROM student_vocabulary_progress WHERE student_id = ?",
            "DELETE FROM pronunciation_practice_logs WHERE student_id = ?",
            "DELETE FROM student_phoneme_bookmarks WHERE student_id = ?",
            "DELETE FROM daily_goals WHERE student_id = ?",
            "DELETE FROM xp_transactions WHERE student_id = ?",
            "DELETE FROM student_stats WHERE student_id = ?",
            "DELETE FROM failed_jobs WHERE student_id = ?",
            "DELETE FROM idempotency_keys WHERE user_id = ?"
    );

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final StoredFileStore storedFileStore;

    @Transactional
    public void purge(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        if (user.getDeletedAt() != null) {
            return;
        }
        if (!Role.STUDENT.equals(user.getRole())) {
            throw ErrorCode.ACCESS_DENIED.toException();
        }

        queueOwnedFilesForDeletion(userId, user.getAvatarUrl());

        int deletedRows = 0;
        for (String sql : DELETE_PERSONAL_DATA_SQL) {
            deletedRows += jdbcTemplate.update(sql, userId);
        }

        // Billing records are retained, but any active entitlement is cancelled.
        jdbcTemplate.update("""
                UPDATE user_subscriptions
                   SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP
                 WHERE user_id = ? AND status IN ('PENDING_PAYMENT', 'ACTIVE')
                """, userId);

        String tombstoneId = UUID.randomUUID().toString();
        user.setEmail("deleted+" + tombstoneId + "@deleted.invalid");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhone(null);
        user.setFullName("Deleted User");
        user.setAvatarUrl(null);
        user.setProvider(AuthProvider.LOCAL);
        user.setProviderId(null);
        user.setLocale("vi");
        user.setIsActive(false);
        user.setOnboardingCompleted(false);
        user.setDeletedAt(LocalDateTime.now());

        log.info("Account data purge completed: userId={}, deletedRows={}", userId, deletedRows);
    }

    private void queueOwnedFilesForDeletion(Long userId, String avatarUrl) {
        List<String> urls = jdbcTemplate.queryForList("""
                SELECT audio_url
                  FROM speaking_turns
                 WHERE audio_url IS NOT NULL
                   AND session_id IN (SELECT id FROM speaking_sessions WHERE student_id = ?)
                UNION
                SELECT audio_url
                  FROM pronunciation_practice_logs
                 WHERE audio_url IS NOT NULL AND student_id = ?
                UNION
                SELECT teacher_audio_comment_url
                  FROM assignment_submissions
                 WHERE teacher_audio_comment_url IS NOT NULL AND student_id = ?
                """, String.class, userId, userId, userId);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            urls.add(avatarUrl);
        }
        urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .distinct()
                .forEach(storedFileStore::queueDeletionByUrl);
    }
}
