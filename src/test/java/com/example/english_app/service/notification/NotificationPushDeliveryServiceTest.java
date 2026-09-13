package com.example.english_app.service.notification;

import com.example.english_app.entity.notification.NotificationOutboxEvent;
import com.example.english_app.repository.notification.NotificationPushDeliveryRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPushDeliveryServiceTest {
    private final NotificationPushDeliveryRepository repository = mock(NotificationPushDeliveryRepository.class);
    private final FcmService fcm = mock(FcmService.class);
    private final NotificationPushDeliveryService service = new NotificationPushDeliveryService(repository,fcm);
    private final NotificationOutboxEvent event = NotificationOutboxEvent.builder().id(1L)
            .claimToken("claim").title("Title").body("Body").build();

    @Test void partialFailurePreservesSuccessfulTokensForRetry() {
        when(repository.findPendingTokens(1L)).thenReturn(List.of("a","b"));
        when(repository.isCurrentClaim(1L,"claim")).thenReturn(true);
        when(fcm.sendToken("a","Title","Body",1L)).thenReturn(true);
        when(repository.markDelivered(1L,"a",true,"claim")).thenReturn(1);
        when(fcm.sendToken("b","Title","Body",1L)).thenThrow(new IllegalStateException("Unavailable"));
        assertThrows(IllegalStateException.class, () -> service.deliver(event));
        verify(repository).markDelivered(1L,"a",true,"claim");
        verify(repository).markFailure(1L,"b","IllegalStateException","claim");
    }

    @Test void staleClaimDoesNotSend() {
        when(repository.findPendingTokens(1L)).thenReturn(List.of("a"));
        assertThrows(IllegalStateException.class, () -> service.deliver(event));
        verifyNoInteractions(fcm);
    }

    @Test void invalidTokenIsRemovedOnlyAfterClaimedDeliveryUpdate() {
        when(repository.findPendingTokens(1L)).thenReturn(List.of("a"));
        when(repository.isCurrentClaim(1L,"claim")).thenReturn(true);
        when(repository.markDelivered(1L,"a",false,"claim")).thenReturn(1);
        service.deliver(event);
        verify(repository).removeInvalidToken("a");
    }

    @Test void losingLeaseDuringSendDoesNotRemoveToken() {
        when(repository.findPendingTokens(1L)).thenReturn(List.of("a"));
        when(repository.isCurrentClaim(1L,"claim")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> service.deliver(event));
        verify(repository,never()).removeInvalidToken(any());
    }
}
