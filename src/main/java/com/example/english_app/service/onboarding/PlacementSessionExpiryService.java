package com.example.english_app.service.onboarding;

import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Commits expiration before the caller returns an error, avoiding rollback of the state change. */
@Service
@RequiredArgsConstructor
public class PlacementSessionExpiryService {

    private final PlacementTestSessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean closeIfExpired(Long sessionId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(PlacementTestSession.SESSION_TIMEOUT_MINUTES);
        return sessionRepository.closeIfExpired(sessionId, userId, cutoff, now) > 0;
    }
}
