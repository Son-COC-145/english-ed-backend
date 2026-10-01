package com.example.english_app.service.ipa;

import com.example.english_app.event.PronunciationCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PronunciationEventListenerTest {

    /** XP must only be granted for a practice log that was actually committed. */
    @Test
    void handlesEventOnlyAfterThePublisherTransactionCommits() throws NoSuchMethodException {
        Method handle = PronunciationEventListener.class.getMethod("handle", PronunciationCompletedEvent.class);

        TransactionalEventListener listener = handle.getAnnotation(TransactionalEventListener.class);
        assertNotNull(listener);
        assertEquals(TransactionPhase.AFTER_COMMIT, listener.phase());
        assertTrue(listener.fallbackExecution());
        assertNull(handle.getAnnotation(EventListener.class));
        assertNotNull(handle.getAnnotation(Async.class));
    }

    @Test
    void delegatesToRetryableXpService() {
        RetryableGamificationService xpService = mock(RetryableGamificationService.class);
        PronunciationCompletedEvent event = new PronunciationCompletedEvent(1L, 20, 42L);

        new PronunciationEventListener(xpService).handle(event);

        verify(xpService).addXp(event);
    }
}
