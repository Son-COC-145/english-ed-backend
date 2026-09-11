package com.example.english_app.service.speaking;

import com.example.english_app.entity.enums.CefrLevel;

import com.example.english_app.dto.request.SpeakingScenarioRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.SpeakingScenarioResponse;
import com.example.english_app.entity.speaking.SpeakingScenario;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.SpeakingMapper;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeakingScenarioService {

    private final SpeakingScenarioRepository speakingScenarioRepository;
    private final TopicRepository topicRepository;
    private final SpeakingMapper speakingMapper;
    private final SpeakingJson json;

    @Transactional(readOnly = true)
    public PageResponse<SpeakingScenarioResponse> filterScenarios(Short id, String title, Short topicId,
            Boolean isActive, CefrLevel cefrLevel, Pageable pageable) {
        Page<SpeakingScenario> speakingScenarios = speakingScenarioRepository.filterScenarios(id, title, topicId,
                !SpeakingAccess.managesScenarios(),
                SpeakingAccess.managesScenarios() ? isActive : Boolean.TRUE, cefrLevel, pageable);
        return PageResponse.of(speakingScenarios.map(this::visibleResponse));
    }

    @Transactional(readOnly = true)
    public SpeakingScenarioResponse getScenarioById(Short id) {
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());
        if (!SpeakingAccess.managesScenarios()
                && (!Boolean.TRUE.equals(scenario.getIsActive())
                        || (scenario.getTopic() != null && !Boolean.TRUE.equals(scenario.getTopic().getIsActive()))))
            throw ErrorCode.SCENARIO_NOT_FOUND.toException();
        return visibleResponse(scenario);
    }

    public SpeakingScenarioResponse create(SpeakingScenarioRequest request) {
        validate(request);
        if (speakingScenarioRepository.existsByTitleVi(request.getTitleVi())
                || speakingScenarioRepository.existsByTitleEn(request.getTitleEn())) {
            throw ErrorCode.SCENARIO_ALREADY_EXISTS.toException();
        }

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());
        }

        SpeakingScenario scenario = SpeakingScenario.builder()
                .titleVi(request.getTitleVi())
                .titleEn(request.getTitleEn())
                .contextDescription(request.getContextDescription())
                .aiRoleName(request.getAiRoleName())
                .aiRoleAvatarUrl(request.getAiRoleAvatarUrl())
                .aiSystemPrompt(request.getAiSystemPrompt())
                .goalDescription(request.getGoalDescription())
                .hintPhrasesJson(request.getHintPhrasesJson())
                .cefrLevel(request.getCefrLevel())
                .topic(topic)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        scenario = speakingScenarioRepository.save(scenario);
        return speakingMapper.toScenarioResponse(scenario);
    }

    public SpeakingScenarioResponse update(Short id, SpeakingScenarioRequest request) {
        validate(request);
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());

        if (speakingScenarioRepository.existsByTitleViAndIdNot(request.getTitleVi(), id)
                || speakingScenarioRepository.existsByTitleEnAndIdNot(request.getTitleEn(), id)) {
            throw ErrorCode.SCENARIO_ALREADY_EXISTS.toException();
        }

        Topic topic = null;
        if (request.getTopicId() != null) {
            topic = topicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());
        }

        scenario.setTitleEn(request.getTitleEn());
        scenario.setTitleVi(request.getTitleVi());
        scenario.setContextDescription(request.getContextDescription());
        scenario.setAiRoleName(request.getAiRoleName());
        scenario.setAiRoleAvatarUrl(request.getAiRoleAvatarUrl());
        scenario.setAiSystemPrompt(request.getAiSystemPrompt());
        scenario.setGoalDescription(request.getGoalDescription());
        scenario.setHintPhrasesJson(request.getHintPhrasesJson());
        scenario.setCefrLevel(request.getCefrLevel());
        scenario.setTopic(topic);
        if (request.getIsActive() != null) {
            scenario.setIsActive(request.getIsActive());
        }

        scenario = speakingScenarioRepository.save(scenario);
        return speakingMapper.toScenarioResponse(scenario);
    }

    public void delete(Short id) {
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());
        speakingScenarioRepository.delete(scenario);
    }

    public void activate(Short id) {
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());
        scenario.setIsActive(true);
        speakingScenarioRepository.save(scenario);
    }

    public void deactivate(Short id) {
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());
        scenario.setIsActive(false);
        speakingScenarioRepository.save(scenario);
    }

    private SpeakingScenarioResponse visibleResponse(SpeakingScenario scenario) {
        var response = speakingMapper.toScenarioResponse(scenario);
        if (!SpeakingAccess.managesScenarios()) {
            response.setHintPhrases(null);
            response.setAiSystemPrompt(null);
        }
        return response;
    }

    private void validate(SpeakingScenarioRequest request) {
        try {
            var hints = json.read(request.getHintPhrasesJson());
            if (!hints.isArray() || hints.size() < 3 || hints.size() > 5)
                throw ErrorCode.INVALID_REQUEST.toException();
            for (var hint : hints)
                if (!hint.isTextual() || hint.asText().isBlank() || hint.asText().length() > 250)
                    throw ErrorCode.INVALID_REQUEST.toException();
            var uniqueHints = new java.util.HashSet<String>();
            for (var hint : hints)
                if (!uniqueHints.add(hint.asText().strip().toLowerCase(java.util.Locale.ROOT)))
                    throw ErrorCode.INVALID_REQUEST.toException();
            if (request.getContextDescription().length() > 2000
                    || request.getAiRoleName().length() > 200
                    || request.getAiSystemPrompt().length() > 8000
                    || request.getGoalDescription().length() > 2000)
                throw ErrorCode.INVALID_REQUEST.toException();
        } catch (IllegalArgumentException e) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }
}
