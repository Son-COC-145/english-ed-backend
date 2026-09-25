package com.example.english_app.service.onboarding;

import com.example.english_app.entity.question.Question;
import com.example.english_app.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a strict learner-facing content contract and prevents internal answer/media data leaks. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlacementQuestionContentMapper {

    private final ObjectMapper objectMapper;

    public Map<String, Object> toLearnerContent(Question question) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(question.getContentJson());
        } catch (Exception exception) {
            log.error("Invalid placement content JSON for question {}", question.getId(), exception);
            throw ErrorCode.SYSTEM_ERROR.toException();
        }

        Map<String, Object> content = new LinkedHashMap<>();
        switch (question.getQuestionType()) {
            case MULTIPLE_CHOICE -> {
                copyRequired(root, content, "question", question);
                copyRequired(root, content, "options", question);
            }
            case FILL_BLANK -> {
                copyRequired(root, content, "question", question);
                copyOptional(root, content, "options");
            }
            case READING_COMPREHENSION -> {
                copyRequiredAlias(root, content, "passage", "text", question);
                copyRequired(root, content, "question", question);
                copyRequired(root, content, "options", question);
            }
            case LISTENING -> {
                if (question.getPlacementAudioUrl() == null || question.getPlacementAudioUrl().isBlank()) {
                    log.error("Listening question {} has no pre-generated placement audio", question.getId());
                    throw ErrorCode.PLACEMENT_QUESTION_EXHAUSTED.toException();
                }
                content.put("audioUrl", question.getPlacementAudioUrl());
                copyRequired(root, content, "question", question);
                copyRequired(root, content, "options", question);
            }
            case PRONUNCIATION -> {
                copyRequired(root, content, "word", question);
                copyRequiredAlias(root, content, "ipaTranscription", "ipa", question);
                if (root.hasNonNull("instruction")) {
                    copyOptional(root, content, "instruction");
                } else {
                    content.put("instruction", "Đọc to từ bên dưới vào microphone");
                }
                copyOptional(root, content, "audioGuideUrl");
            }
        }
        return content;
    }

    private void copyRequired(JsonNode root, Map<String, Object> target, String field, Question question) {
        if (!root.hasNonNull(field)) {
            invalidContent(question, field);
        }
        target.put(field, objectMapper.convertValue(root.get(field), Object.class));
    }

    private void copyRequiredAlias(
            JsonNode root,
            Map<String, Object> target,
            String canonical,
            String legacy,
            Question question) {
        JsonNode value = root.hasNonNull(canonical) ? root.get(canonical) : root.get(legacy);
        if (value == null || value.isNull()) {
            invalidContent(question, canonical);
        }
        target.put(canonical, objectMapper.convertValue(value, Object.class));
    }

    private void copyOptional(JsonNode root, Map<String, Object> target, String field) {
        if (root.hasNonNull(field)) {
            target.put(field, objectMapper.convertValue(root.get(field), Object.class));
        }
    }

    private void invalidContent(Question question, String field) {
        log.error("Placement question {} ({}) is missing required field '{}'",
                question.getId(), question.getQuestionType(), field);
        throw ErrorCode.SYSTEM_ERROR.toException();
    }
}
