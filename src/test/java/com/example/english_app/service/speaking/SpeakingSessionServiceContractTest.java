package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.SpeakerRole;
import com.example.english_app.entity.speaking.SpeakingSession;
import com.example.english_app.entity.speaking.SpeakingTurn;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.service.integration.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpeakingSessionServiceContractTest {
    final SpeakingStore store = mock(SpeakingStore.class);
    final SpeakingAccess access = mock(SpeakingAccess.class);
    final ObjectMapper mapper = new ObjectMapper();
    final SpeakingSessionService service = new SpeakingSessionService(store, access,
            new SpeakingMapper(mapper), new SpeakingJson(mapper), mock(CloudinaryService.class));
    SpeakingSession session;

    @BeforeEach void setup() {
        session = SpeakingSession.builder().id(1L).student(User.builder().id(7L).build()).build();
        when(access.userId()).thenReturn(7L);
        when(store.owned(1L, 7L)).thenReturn(session);
    }

    private void audioError(SpeakingTurn turn, ErrorCode code) {
        when(store.turn(2L)).thenReturn(turn);
        assertThatThrownBy(() -> service.audio(1L, 2L)).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }

    @Test void audioReadinessErrorsAreDistinct() {
        var turn = SpeakingTurn.builder().id(2L).session(session).speaker(SpeakerRole.AI).build();
        audioError(turn, ErrorCode.SPEAKING_AUDIO_PENDING);
        turn.setAudioStatus("FAILED");
        audioError(turn, ErrorCode.SPEAKING_AUDIO_FAILED);
        turn.setAudioStatus("READY");
        audioError(turn, ErrorCode.SPEAKING_AUDIO_MISSING);
        turn.setAudioData(new byte[]{1, 2});
        assertThat(service.audio(1L, 2L)).isSameAs(turn);
    }

    @Test void wrongSessionDoesNotExposeAudioReadiness() {
        var otherSession = SpeakingSession.builder().id(9L).build();
        audioError(SpeakingTurn.builder().session(otherSession).speaker(SpeakerRole.AI).build(), ErrorCode.SESSION_NOT_FOUND);
    }

    @Test void unknownAudioDoesNotCreateTurnOrUpload() {
        var file = new MockMultipartFile("file", "recording.wav", "audio/wav", new byte[64]);
        assertThatThrownBy(() -> service.processUserAudio(1L, "logical-key", file)).isInstanceOfSatisfying(AppException.class,
                ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_AUDIO_FORMAT));
        verify(store, never()).submit(any(), any(), any(), any(), any(), any(), any());
    }

    @Test void mp3InputReachesStoreWithDetectedMimeAndOriginalBytes() {
        byte[] audio = new byte[64];
        audio[0] = 'I'; audio[1] = 'D'; audio[2] = '3';
        var turn = SpeakingTurn.builder().id(2L).session(session).speaker(SpeakerRole.STUDENT)
                .audioUrl("https://example.test/recording.mp3").build();
        when(store.submit(eq(1L), eq(7L), eq("logical-key"), anyString(), eq(audio), eq("audio/mpeg"), isNull()))
                .thenReturn(turn);
        var file = new MockMultipartFile("file", "recording.mp3", "application/octet-stream", audio);
        assertThat(service.processUserAudio(1L, "logical-key", file).turnId()).isEqualTo(2L);
        verify(store).submit(eq(1L), eq(7L), eq("logical-key"), anyString(), eq(audio), eq("audio/mpeg"), isNull());
    }

    @Test void finalTextSnapshotIsConsistentAndHasDbAudioFallback() {
        var turn = SpeakingTurn.builder().id(2L).session(session).turnIndex(1).speaker(SpeakerRole.AI)
                .responseTextReady(true).transcriptText("Final text while TTS is pending").build();
        when(store.history(1L)).thenReturn(List.of(turn));
        var snapshot = service.streamingSnapshot(1L, 7L);
        assertThat(snapshot.finalizedAiTurnIds()).containsExactly(2L);
        assertThat(snapshot.report().getTurns().getFirst().getStatus()).isEqualTo("PENDING");
        turn.setAudioData(new byte[]{1});
        assertThat(service.report(1L).getTurns().getFirst().getAudioUrl())
                .isEqualTo("/api/v1/speaking-session/1/turns/2/audio");
    }

    @Test void startKeyRecoveryIsReadOnly() {
        when(store.recover(7L, "start-key")).thenReturn(session);
        when(store.history(1L)).thenReturn(List.of());
        assertThat(service.recover("start-key").getSessionId()).isEqualTo(1L);
        verify(store, never()).start(any(), any(), any());
        verify(store, never()).end(any(), any());
    }
}
