package com.example.english_app.service.speaking;

import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.speaking.SpeakingStartRequestRepository;
import com.example.english_app.repository.speaking.SpeakingTurnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/** Requests already committed in the database bypass quota. */
@Component
@RequiredArgsConstructor
public class SpeakingQuotaIdentity {
    private static final Pattern INPUT = Pattern.compile(".*/speaking-session/(\\d+)/(?:audio|text)-input$");
    private final SpeakingStartRequestRepository starts;
    private final SpeakingSessionRepository sessions;
    private final SpeakingTurnRepository turns;

    public boolean committed(Long userId, String path, String key) {
        if (path.endsWith("/speaking-session/start")) {
            return starts.findByStudentIdAndRequestKey(userId, key)
                    .filter(request -> request.getSession() != null).isPresent();
        }
        var matcher = INPUT.matcher(path);
        if (!matcher.matches()) return false;
        Long id = Long.valueOf(matcher.group(1));
        return sessions.findById(id).filter(s -> s.getStudent().getId().equals(userId)).isPresent()
                && turns.findBySessionIdAndRequestKey(id, key).isPresent();
    }
}
