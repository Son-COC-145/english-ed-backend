package com.example.english_app.service.speaking;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class SpeakingScenarioService {

    private final SpeakingScenarioRepository speakingScenarioRepository;
    private final TopicRepository topicRepository;
    private final SpeakingMapper speakingMapper;

    public PageResponse<SpeakingScenarioResponse> filterScenarios(Short id, String title, Short topicId,
            Boolean isActive, Pageable pageable) {
        Page<SpeakingScenario> speakingScenarios = speakingScenarioRepository.filterScenarios(id, title, topicId,
                isActive, pageable);
        return PageResponse.of(speakingScenarios.map(speakingMapper::toScenarioResponse));
    }

    public SpeakingScenarioResponse getScenarioById(Short id) {
        SpeakingScenario scenario = speakingScenarioRepository.findById(id)
                .orElseThrow(() -> ErrorCode.SCENARIO_NOT_FOUND.toException());
        return speakingMapper.toScenarioResponse(scenario);
    }

    public SpeakingScenarioResponse create(SpeakingScenarioRequest request) {
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
}
