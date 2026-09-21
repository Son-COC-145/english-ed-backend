package com.example.english_app.service.speaking;

import com.example.english_app.entity.speaking.*;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.speaking.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpeakingQuotaIdentityTest {
    final SpeakingStartRequestRepository starts = mock(SpeakingStartRequestRepository.class);
    final SpeakingSessionRepository sessions = mock(SpeakingSessionRepository.class);
    final SpeakingTurnRepository turns = mock(SpeakingTurnRepository.class);
    final SpeakingQuotaIdentity identity = new SpeakingQuotaIdentity(starts, sessions, turns);

    @Test void committedInputBypassesQuotaOnlyForOwner() {
        when(sessions.findById(42L)).thenReturn(Optional.of(SpeakingSession.builder().id(42L)
                .student(User.builder().id(7L).build()).build()));
        when(turns.findBySessionIdAndRequestKey(42L, "logicalkey"))
                .thenReturn(Optional.of(SpeakingTurn.builder().id(1L).build()));
        assertThat(identity.committed(7L, "/api/v1/speaking-session/42/audio-input", "logicalkey")).isTrue();
        assertThat(identity.committed(8L, "/api/v1/speaking-session/42/audio-input", "logicalkey")).isFalse();
    }

    @Test void unboundStartClaimDoesNotBypassQuota() {
        when(starts.findByStudentIdAndRequestKey(7L, "logicalkey"))
                .thenReturn(Optional.of(SpeakingStartRequest.builder().build()));
        assertThat(identity.committed(7L, "/api/v1/speaking-session/start", "logicalkey")).isFalse();
    }
}
