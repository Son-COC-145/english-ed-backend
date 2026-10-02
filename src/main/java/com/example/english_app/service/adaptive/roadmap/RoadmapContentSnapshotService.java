package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.config.AdaptiveRoadmapProperties;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoadmapContentSnapshotService {

    public static final String VOCABULARY = "VOCABULARY";
    public static final String SPEAKING = "SPEAKING";
    public static final String IPA_PRONUNCIATION = "IPA_PRONUNCIATION";

    private static final String IPA_VOWELS_BASIC = "IPA_VOWELS_BASIC";
    private static final String IPA_DIPHTHONGS = "IPA_DIPHTHONGS";
    private static final String IPA_CONSONANTS_1 = "IPA_CONSONANTS_1";
    private static final String IPA_CONSONANTS_2 = "IPA_CONSONANTS_2";

    private final VocabularyRepository vocabularyRepository;
    private final SpeakingScenarioRepository speakingScenarioRepository;
    private final IpaPhonemeRepository ipaPhonemeRepository;
    private final AdaptiveRoadmapProperties properties;

    public Optional<RoadmapModule> vocabularyModule(Topic topic, CefrLevel fallbackLevel) {
        List<Long> contentIds = vocabularyIds(topic.getId());
        if (contentIds.isEmpty()) return Optional.empty();

        CefrLevel level = topic.getCefrLevel() != null ? topic.getCefrLevel() : fallbackLevel;
        return Optional.of(module(
                VOCABULARY,
                "Từ vựng: " + topic.getNameVi(),
                topic.getId(),
                level,
                VOCABULARY + ":" + topic.getId(),
                contentIds));
    }

    public Optional<RoadmapModule> speakingModule(Topic topic, CefrLevel level) {
        List<Long> contentIds = speakingScenarioIds(topic.getId(), level);
        if (contentIds.isEmpty()) return Optional.empty();

        return Optional.of(module(
                SPEAKING,
                "Giao tiếp thực tế: " + topic.getNameVi(),
                topic.getId(),
                level,
                SPEAKING + ":" + topic.getId(),
                contentIds));
    }

    public List<IpaModuleGroup> ipaModuleGroups(CefrLevel level) {
        Map<PhonemeType, List<Long>> idsByType = new LinkedHashMap<>();
        for (PhonemeType type : PhonemeType.values()) {
            idsByType.put(type, new ArrayList<>());
        }
        for (IpaPhoneme phoneme : ipaPhonemeRepository.findAllByOrderByIdAsc()) {
            if (phoneme.getId() != null && phoneme.getPhonemeType() != null) {
                idsByType.get(phoneme.getPhonemeType()).add(phoneme.getId().longValue());
            }
        }

        List<Long> consonants = idsByType.get(PhonemeType.CONSONANT);
        int midpoint = (consonants.size() + 1) / 2;
        List<Long> firstConsonants = new ArrayList<>(consonants.subList(0, midpoint));
        List<Long> secondConsonants = new ArrayList<>(consonants.subList(midpoint, consonants.size()));
        secondConsonants.addAll(idsByType.get(PhonemeType.SPECIAL));

        List<IpaModuleGroup> groups = new ArrayList<>();
        addGroup(groups, IPA_VOWELS_BASIC, "Nguyên âm đơn IPA", idsByType.get(PhonemeType.VOWEL_MONO), level);
        addGroup(groups, IPA_DIPHTHONGS, "Nguyên âm đôi IPA", idsByType.get(PhonemeType.VOWEL_DIPH), level);
        addGroup(groups, IPA_CONSONANTS_1, "Phụ âm IPA - nhóm 1", firstConsonants, level);
        addGroup(groups, IPA_CONSONANTS_2, "Phụ âm IPA - nhóm 2", secondConsonants, level);
        return List.copyOf(groups);
    }

    /** Adds the immutable snapshot fields to a non-IPA module read from legacy JSON. */
    public void resolveLegacyModule(
            RoadmapModule module,
            CefrLevel roadmapLevel,
            int weekNumber,
            int moduleIndex) {
        if (isSnapshotComplete(module)) return;

        List<Long> contentIds;
        String moduleKey;
        if (VOCABULARY.equals(module.getType()) && module.getTopicId() != null) {
            contentIds = vocabularyIds(module.getTopicId());
            moduleKey = VOCABULARY + ":" + module.getTopicId();
        } else if (SPEAKING.equals(module.getType()) && module.getTopicId() != null) {
            contentIds = speakingScenarioIds(module.getTopicId(), parseLevel(module.getCefrLevel(), roadmapLevel));
            moduleKey = SPEAKING + ":" + module.getTopicId();
        } else if (IPA_PRONUNCIATION.equals(module.getType()) && module.getModuleKey() != null) {
            Optional<RoadmapModule> current = ipaModuleGroups(roadmapLevel).stream()
                    .flatMap(group -> group.modules().stream())
                    .filter(candidate -> candidate.getModuleKey().equals(module.getModuleKey()))
                    .findFirst();
            contentIds = current.map(RoadmapModule::getContentItemIds).orElseGet(List::of);
            moduleKey = module.getModuleKey();
        } else {
            contentIds = List.of();
            moduleKey = defaultModuleKey(module, weekNumber, moduleIndex);
        }

        applySnapshot(module, moduleKey, contentIds);
    }

    public boolean isSnapshotComplete(RoadmapModule module) {
        return module.getModuleKey() != null
                && module.getContentItemIds() != null
                && module.getContentVersion() != null;
    }

    public String contentVersion(List<Long> contentItemIds) {
        String canonical = canonicalIds(contentItemIds).stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private List<Long> vocabularyIds(Short topicId) {
        return canonicalIds(vocabularyRepository.findPublishedIdsByTopicId(topicId));
    }

    private List<Long> speakingScenarioIds(Short topicId, CefrLevel level) {
        return canonicalIds(speakingScenarioRepository.findActiveIdsForRoadmap(level, topicId).stream()
                .map(Short::longValue)
                .toList());
    }

    private RoadmapModule module(
            String type,
            String title,
            Short topicId,
            CefrLevel level,
            String moduleKey,
            List<Long> contentIds) {
        List<Long> canonicalIds = canonicalIds(contentIds);
        return RoadmapModule.builder()
                .type(type)
                .title(title)
                .topicId(topicId)
                .itemCount(canonicalIds.size())
                .cefrLevel(level.name())
                .moduleKey(moduleKey)
                .contentItemIds(canonicalIds)
                .contentVersion(contentVersion(canonicalIds))
                .build();
    }

    private void addGroup(
            List<IpaModuleGroup> groups,
            String groupKey,
            String title,
            List<Long> rawIds,
            CefrLevel level) {
        List<Long> ids = canonicalIds(rawIds);
        if (ids.isEmpty()) return;

        int maxSize = properties.getIpaMaxPerModule();
        int chunkCount = (ids.size() + maxSize - 1) / maxSize;
        List<RoadmapModule> modules = new ArrayList<>(chunkCount);
        for (int start = 0, chunk = 1; start < ids.size(); start += maxSize, chunk++) {
            List<Long> chunkIds = List.copyOf(ids.subList(start, Math.min(start + maxSize, ids.size())));
            String suffix = chunkCount == 1 ? "" : ":" + chunk;
            String titleSuffix = chunkCount == 1 ? "" : " (phần " + chunk + ")";
            modules.add(module(
                    IPA_PRONUNCIATION,
                    title + titleSuffix,
                    null,
                    level,
                    "IPA:" + groupKey + suffix,
                    chunkIds));
        }
        groups.add(new IpaModuleGroup(groupKey, List.copyOf(modules)));
    }

    private void applySnapshot(RoadmapModule module, String moduleKey, List<Long> rawIds) {
        List<Long> ids = canonicalIds(rawIds);
        module.setModuleKey(moduleKey);
        module.setContentItemIds(ids);
        module.setContentVersion(contentVersion(ids));
        module.setItemCount(ids.size());
    }

    private List<Long> canonicalIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private CefrLevel parseLevel(String value, CefrLevel fallback) {
        if (value == null) return fallback;
        try {
            return CefrLevel.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private String defaultModuleKey(RoadmapModule module, int weekNumber, int moduleIndex) {
        String type = module.getType() == null || module.getType().isBlank()
                ? "UNKNOWN"
                : module.getType();
        if (module.getTopicId() != null) return type + ":" + module.getTopicId();
        return type + ":W" + weekNumber + ":M" + moduleIndex;
    }

    public record IpaModuleGroup(String key, List<RoadmapModule> modules) {
    }
}
