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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapContentSnapshotServiceTest {

    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private SpeakingScenarioRepository speakingScenarioRepository;
    @Mock private IpaPhonemeRepository ipaPhonemeRepository;

    private AdaptiveRoadmapProperties properties;
    private RoadmapContentSnapshotService service;

    @BeforeEach
    void setUp() {
        properties = new AdaptiveRoadmapProperties();
        service = new RoadmapContentSnapshotService(
                vocabularyRepository, speakingScenarioRepository, ipaPhonemeRepository, properties);
    }

    @Test
    void vocabularySnapshotIsSortedDistinctAndVersionIsStable() {
        Topic topic = Topic.builder()
                .id((short) 7)
                .nameVi("Du lịch")
                .cefrLevel(CefrLevel.A2)
                .build();
        when(vocabularyRepository.findPublishedIdsByTopicId((short) 7))
                .thenReturn(List.of(9L, 2L, 9L, 4L));

        RoadmapModule module = service.vocabularyModule(topic, CefrLevel.A1).orElseThrow();

        assertThat(module.getModuleKey()).isEqualTo("VOCABULARY:7");
        assertThat(module.getContentItemIds()).containsExactly(2L, 4L, 9L);
        assertThat(module.getItemCount()).isEqualTo(3);
        assertThat(module.getContentVersion())
                .isEqualTo(service.contentVersion(List.of(4L, 9L, 2L, 4L)))
                .hasSize(32);
    }

    @Test
    void ipaGroupsNeverExceedConfiguredMaximumAndCoverEveryPhonemeOnce() {
        properties.setIpaMaxPerModule(3);
        when(ipaPhonemeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                phoneme(1, PhonemeType.VOWEL_MONO),
                phoneme(2, PhonemeType.VOWEL_MONO),
                phoneme(3, PhonemeType.VOWEL_MONO),
                phoneme(4, PhonemeType.VOWEL_MONO),
                phoneme(5, PhonemeType.VOWEL_DIPH),
                phoneme(6, PhonemeType.CONSONANT),
                phoneme(7, PhonemeType.CONSONANT),
                phoneme(8, PhonemeType.CONSONANT),
                phoneme(9, PhonemeType.CONSONANT),
                phoneme(10, PhonemeType.CONSONANT),
                phoneme(11, PhonemeType.SPECIAL)));

        List<RoadmapContentSnapshotService.IpaModuleGroup> groups =
                service.ipaModuleGroups(CefrLevel.A1);

        assertThat(groups).extracting(RoadmapContentSnapshotService.IpaModuleGroup::key)
                .containsExactly(
                        "IPA_VOWELS_BASIC",
                        "IPA_DIPHTHONGS",
                        "IPA_CONSONANTS_1",
                        "IPA_CONSONANTS_2");
        assertThat(groups).flatExtracting(RoadmapContentSnapshotService.IpaModuleGroup::modules)
                .allSatisfy(module -> {
                    assertThat(module.getContentItemIds()).hasSizeLessThanOrEqualTo(3);
                    assertThat(module.getItemCount()).isEqualTo(module.getContentItemIds().size());
                });
        assertThat(groups).flatExtracting(RoadmapContentSnapshotService.IpaModuleGroup::modules)
                .flatExtracting(RoadmapModule::getContentItemIds)
                .containsExactlyInAnyOrderElementsOf(
                        List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));
    }

    private IpaPhoneme phoneme(int id, PhonemeType type) {
        return IpaPhoneme.builder().id((short) id).phonemeType(type).build();
    }
}
