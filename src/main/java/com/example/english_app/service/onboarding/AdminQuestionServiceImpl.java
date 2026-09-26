package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.AdminQuestionRequest;
import com.example.english_app.dto.response.AdminQuestionResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.QuestionBankStatsResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.service.audio.AzureTtsService;
import com.example.english_app.service.storage.AzureBlobStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminQuestionServiceImpl implements AdminQuestionService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final AzureTtsService azureTtsService;
    private final AzureBlobStorageService azureBlobStorageService;

    // ─── List / Detail ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminQuestionResponse> getQuestions(CefrLevel level, Skill skill, Boolean isActive, Pageable pageable) {
        Page<Question> page = questionRepository.findByFilters(level, skill, isActive, pageable);
        return PageResponse.of(page.map(this::mapToResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminQuestionResponse getQuestionById(Long id) {
        return mapToResponse(findOrThrow(id));
    }

    // ─── Create / Update / Delete ──────────────────────────────────────────────

    @Override
    @Transactional
    public AdminQuestionResponse createQuestion(AdminQuestionRequest request) {
        validateSkillAndType(request.getSkill(), request.getQuestionType());
        NormalizedContent normalized = normalizeAndValidateContent(
                request.getQuestionType(), request.getContentJson(), request.getCorrectAnswer());
        boolean active = request.getIsActive() != null ? request.getIsActive() : true;
        validateListeningActivation(request.getQuestionType(), active, normalized.audioUrl());
        Question q = Question.builder()
                .cefrLevel(request.getCefrLevel())
                .skill(request.getSkill())
                .questionType(request.getQuestionType())
                .contentJson(normalized.contentJson())
                .correctAnswer(request.getCorrectAnswer())
                .timeoutSeconds(request.getTimeoutSeconds())
                .difficultyIndex(request.getDifficultyIndex())
                .placementAudioUrl(normalized.audioUrl())
                .isActive(active)
                .build();

        return mapToResponse(questionRepository.save(q));
    }

    @Override
    @Transactional
    public AdminQuestionResponse updateQuestion(Long id, AdminQuestionRequest request) {
        validateSkillAndType(request.getSkill(), request.getQuestionType());
        NormalizedContent normalized = normalizeAndValidateContent(
                request.getQuestionType(), request.getContentJson(), request.getCorrectAnswer());
        Question q = findOrThrow(id);
        boolean active = request.getIsActive() != null ? request.getIsActive() : q.getIsActive();
        validateListeningActivation(request.getQuestionType(), active, normalized.audioUrl());

        q.setCefrLevel(request.getCefrLevel());
        q.setSkill(request.getSkill());
        q.setQuestionType(request.getQuestionType());
        q.setContentJson(normalized.contentJson());
        q.setCorrectAnswer(request.getCorrectAnswer());
        q.setTimeoutSeconds(request.getTimeoutSeconds());
        if (request.getDifficultyIndex() != null) q.setDifficultyIndex(request.getDifficultyIndex());
        q.setIsActive(active);
        if (request.getQuestionType() == QuestionType.LISTENING) {
            // Any content edit invalidates the old generated recording unless a new opaque URL
            // was explicitly supplied by the administrator.
            q.setPlacementAudioUrl(normalized.audioUrl());
        } else {
            q.setPlacementAudioUrl(null);
        }

        return mapToResponse(questionRepository.save(q));
    }

    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new AppException(ErrorCode.PLACEMENT_TEST_NOT_FOUND);
        }
        questionRepository.deleteById(id);
    }

    // ─── Toggle Active ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminQuestionResponse toggleActive(Long id, boolean active) {
        Question q = findOrThrow(id);
        validateListeningActivation(q.getQuestionType(), active, q.getPlacementAudioUrl());
        q.setIsActive(active);
        return mapToResponse(questionRepository.save(q));
    }

    // ─── Stats ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public QuestionBankStatsResponse getStats() {
        long totalActive   = questionRepository.countByIsActiveTrue();
        long totalInactive = questionRepository.countByIsActiveFalse();

        // Breakdown theo level
        Map<CefrLevel, Long> byLevel = new EnumMap<>(CefrLevel.class);
        for (Object[] row : questionRepository.countActiveGroupByLevel()) {
            byLevel.put((CefrLevel) row[0], (Long) row[1]);
        }

        // Breakdown theo skill
        Map<Skill, Long> bySkill = new EnumMap<>(Skill.class);
        for (Object[] row : questionRepository.countActiveGroupBySkill()) {
            bySkill.put((Skill) row[0], (Long) row[1]);
        }

        // Breakdown chi tiết (level × skill × active)
        List<QuestionBankStatsResponse.CellStat> breakdown = questionRepository
                .countGroupByLevelAndSkillAndActive().stream()
                .collect(
                    java.util.stream.Collectors.groupingBy(
                        row -> row[0].toString() + "|" + row[1].toString()
                    )
                )
                .entrySet().stream()
                .map(entry -> {
                    List<Object[]> rows = entry.getValue();
                    CefrLevel level = (CefrLevel) rows.get(0)[0];
                    Skill skill     = (Skill)     rows.get(0)[1];
                    long activeCount   = 0L;
                    long inactiveCount = 0L;
                    for (Object[] row : rows) {
                        boolean isActive = (Boolean) row[2];
                        long cnt = (Long) row[3];
                        if (isActive) activeCount = cnt;
                        else inactiveCount = cnt;
                    }
                    return QuestionBankStatsResponse.CellStat.builder()
                            .cefrLevel(level)
                            .skill(skill)
                            .activeCount(activeCount)
                            .inactiveCount(inactiveCount)
                            .build();
                })
                .sorted(java.util.Comparator
                        .comparing(QuestionBankStatsResponse.CellStat::getCefrLevel)
                        .thenComparing(QuestionBankStatsResponse.CellStat::getSkill))
                .toList();

        return QuestionBankStatsResponse.builder()
                .totalActive(totalActive)
                .totalInactive(totalInactive)
                .byLevel(byLevel)
                .bySkill(bySkill)
                .breakdown(breakdown)
                .build();
    }

    // ─── Helper ────────────────────────────────────────────────────────────────

    private Question findOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PLACEMENT_TEST_NOT_FOUND));
    }

    private AdminQuestionResponse mapToResponse(Question q) {
        return AdminQuestionResponse.builder()
                .id(q.getId())
                .cefrLevel(q.getCefrLevel())
                .skill(q.getSkill())
                .questionType(q.getQuestionType())
                .contentJson(q.getContentJson())
                .correctAnswer(q.getCorrectAnswer())
                .timeoutSeconds(q.getTimeoutSeconds())
                .difficultyIndex(q.getDifficultyIndex())
                .placementAudioUrl(q.getPlacementAudioUrl())
                .isActive(q.getIsActive())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    /**
     * Kiểm tra {@code contentJson} có chứa các field bắt buộc theo từng {@code questionType}.
     * Ném {@code AppException(INVALID_INPUT)} nếu JSON không hợp lệ / thiếu field.
     * Giúp Admin biết ngay lỗi schema thay vì lưu data sai vào DB.
     */
    private NormalizedContent normalizeAndValidateContent(
            QuestionType type,
            String contentJson,
            String correctAnswer) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(contentJson);
            if (type == QuestionType.READING_COMPREHENSION && !root.hasNonNull("passage") && root.hasNonNull("text")) {
                root.set("passage", root.get("text"));
            }
            if (type == QuestionType.PRONUNCIATION && !root.hasNonNull("ipaTranscription") && root.hasNonNull("ipa")) {
                root.set("ipaTranscription", root.get("ipa"));
            }
            if (type == QuestionType.LISTENING && !root.hasNonNull("audioUrl") && root.hasNonNull("audio_url")) {
                root.set("audioUrl", root.get("audio_url"));
            }

            String audioUrl = type == QuestionType.LISTENING && root.hasNonNull("audioUrl")
                    ? root.get("audioUrl").asText().trim()
                    : null;

            root.remove("text");
            root.remove("ipa");
            root.remove("audio_url");
            root.remove("audioUrl");

            switch (type) {
                case MULTIPLE_CHOICE -> {
                    requireField(root, "question", type);
                    requireField(root, "options",  type);
                }
                case FILL_BLANK -> requireField(root, "question", type);
                case READING_COMPREHENSION -> {
                    requireField(root, "passage",  type);
                    requireField(root, "question", type);
                    requireField(root, "options",  type);
                }
                case LISTENING -> {
                    requireField(root, "transcript", type);
                    requireField(root, "question", type);
                    requireField(root, "options",  type);
                }
                case PRONUNCIATION -> {
                    requireField(root, "word",             type);
                    requireField(root, "ipaTranscription", type);
                }
            }

            validateOptionsAndAnswer(type, root, correctAnswer);
            return new NormalizedContent(objectMapper.writeValueAsString(root), blankToNull(audioUrl));
        } catch (AppException e) {
            throw e; // re-throw validation errors
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "contentJson không phải JSON hợp lệ: " + e.getMessage());
        }
    }

    private void validateSkillAndType(Skill skill, QuestionType type) {
        boolean valid = switch (type) {
            case LISTENING -> skill == Skill.LISTENING;
            case READING_COMPREHENSION -> skill == Skill.READING;
            case PRONUNCIATION -> skill == Skill.PRONUNCIATION;
            case MULTIPLE_CHOICE, FILL_BLANK -> skill == Skill.VOCABULARY || skill == Skill.GRAMMAR;
        };
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Question type " + type + " is incompatible with skill " + skill);
        }
    }

    private void validateOptionsAndAnswer(QuestionType type, JsonNode root, String correctAnswer) {
        if (type == QuestionType.PRONUNCIATION) {
            String displayedWord = root.get("word").asText().trim();
            if (!displayedWord.equalsIgnoreCase(correctAnswer.trim())) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "correctAnswer must match contentJson.word for pronunciation");
            }
            return;
        }
        if (type == QuestionType.FILL_BLANK) return;
        JsonNode options = root.get("options");
        if (options == null || !options.isArray() || options.size() < 2) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "contentJson options must contain at least two choices");
        }
        boolean matches = false;
        java.util.Set<String> unique = new java.util.HashSet<>();
        for (JsonNode option : options) {
            String value = option.asText().trim();
            unique.add(value.toLowerCase(java.util.Locale.ROOT));
            if (value.equalsIgnoreCase(correctAnswer.trim())) matches = true;
        }
        if (unique.size() != options.size()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "contentJson options must be unique");
        }
        if (!matches) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "correctAnswer must match one of contentJson.options");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void validateListeningActivation(QuestionType type, boolean active, String audioUrl) {
        if (active && type == QuestionType.LISTENING && blankToNull(audioUrl) == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Listening question must have pre-generated audio before activation");
        }
    }

    private record NormalizedContent(String contentJson, String audioUrl) {
    }

    private void requireField(JsonNode root, String field, QuestionType type) {
        if (!root.has(field) || root.get(field).isNull()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    String.format("contentJson của %s phải có field '%s'", type, field));
        }
    }
}

