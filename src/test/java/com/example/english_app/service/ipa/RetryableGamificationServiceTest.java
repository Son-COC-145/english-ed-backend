package com.example.english_app.service.ipa;

import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.ipa.FailedJob;
import com.example.english_app.event.PronunciationCompletedEvent;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.ipa.FailedJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho RetryableGamificationService — Phase 2: Event-Driven XP.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RetryableGamificationService Unit Tests")
class RetryableGamificationServiceTest {

    @Mock private StudentStatRepository studentStatRepository;
    @Mock private FailedJobRepository   failedJobRepository;

    @InjectMocks
    private RetryableGamificationService gamificationService;

    private PronunciationCompletedEvent event;
    private StudentStat                  mockStat;

    @BeforeEach
    void setUp() {
        event = new PronunciationCompletedEvent(1L, 20, 42L);
        mockStat = StudentStat.builder()
                .totalXp(100)
                .build();
    }

    @Test
    @DisplayName("addXp: studentStat tồn tại → cộng XP chính xác")
    void addXp_statExists_addsCorrectXp() {
        given(studentStatRepository.findByStudentId(1L)).willReturn(Optional.of(mockStat));

        gamificationService.addXp(event);

        assertThat(mockStat.getTotalXp()).isEqualTo(120); // 100 + 20
        verify(studentStatRepository).save(mockStat);
    }

    @Test
    @DisplayName("addXp: studentStat không tồn tại → return sớm, không save, không throw")
    void addXp_statNotExists_returnsGracefully() {
        given(studentStatRepository.findByStudentId(1L)).willReturn(Optional.empty());

        assertThatCode(() -> gamificationService.addXp(event)).doesNotThrowAnyException();
        verify(studentStatRepository, never()).save(any());
    }

    @Test
    @DisplayName("recover: ghi vào failed_jobs với đúng studentId, xpReward, status=PENDING")
    void recover_writesToDlqWithCorrectData() {
        CannotAcquireLockException lockEx = new CannotAcquireLockException("Lock timeout");
        given(failedJobRepository.save(any(FailedJob.class))).willReturn(mock(FailedJob.class));

        gamificationService.recover(lockEx, event);

        ArgumentCaptor<FailedJob> captor = ArgumentCaptor.forClass(FailedJob.class);
        verify(failedJobRepository).save(captor.capture());

        FailedJob savedJob = captor.getValue();
        assertThat(savedJob.getStudentId()).isEqualTo(1L);
        assertThat(savedJob.getStatus()).isEqualTo("PENDING");
        assertThat(savedJob.getPayloadJson()).contains("\"xpReward\":20");
        assertThat(savedJob.getErrorMessage()).contains("Lock timeout");
    }

    @Test
    @DisplayName("recover: DLQ cũng fail → không throw exception (tránh crash async thread)")
    void recover_dlqAlsoFails_doesNotThrow() {
        given(failedJobRepository.save(any())).willThrow(new RuntimeException("DB down"));

        // CRITICAL: không được throw exception ra ngoài vì sẽ crash thread @Async
        assertThatCode(() -> gamificationService.recover(new Exception("original"), event))
                .doesNotThrowAnyException();
    }
}
