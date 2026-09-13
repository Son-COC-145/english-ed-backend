package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.gamification.MinigameResultRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentReferenceService {
    private final IpaExampleWordRepository wordRepository;
    private final TopicRepository topicRepository;
    private final SpeakingScenarioRepository scenarioRepository;
    private final PronunciationPracticeLogRepository practiceRepository;
    private final MinigameResultRepository gameRepository;
    private final SpeakingSessionRepository sessionRepository;

    public void validateTarget(ModuleType moduleType, Long refId) {
        if (moduleType == null || refId == null || refId <= 0) throw ErrorCode.INVALID_REQUEST.toException();
        boolean exists = switch (moduleType) {
            case PRONUNCIATION -> wordRepository.existsById(refId);
            case VOCABULARY -> refId <= Short.MAX_VALUE && topicRepository.existsById(refId.shortValue());
            case SPEAKING -> scenarioRepository.existsById(refId.shortValue()) && refId <= Short.MAX_VALUE;
        };
        if (!exists) throw ErrorCode.INVALID_REQUEST.toException();
    }

    public void validateResult(Assignment assignment, Long studentId, Long resultId) {
        if (resultId == null || resultId <= 0) throw ErrorCode.INVALID_REQUEST.toException();
        boolean matches = switch (assignment.getModuleType()) {
            case PRONUNCIATION -> practiceRepository.findById(resultId)
                    .filter(result -> result.getStudent().getId().equals(studentId)
                            && result.getPracticeType() == PracticeType.WORD
                            && result.getRefId().equals(assignment.getRefId())).isPresent();
            case VOCABULARY -> gameRepository.findById(resultId)
                    .filter(result -> result.getStudent().getId().equals(studentId)
                            && result.getTopic() != null
                            && result.getTopic().getId().longValue() == assignment.getRefId()).isPresent();
            case SPEAKING -> sessionRepository.findById(resultId)
                    .filter(result -> result.getStudent().getId().equals(studentId)
                            && result.getScenario().getId().longValue() == assignment.getRefId()
                            && "COMPLETED".equals(result.getStatus())).isPresent();
        };
        if (!matches) throw ErrorCode.INVALID_REQUEST.toException();
    }
}
