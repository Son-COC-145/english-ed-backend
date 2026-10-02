package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.config.AdaptiveRoadmapProperties;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapContentResolverTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private VocabularyRepository vocabularyRepository;
    @Mock private SpeakingScenarioRepository speakingScenarioRepository;
    @Mock private IpaPhonemeRepository ipaPhonemeRepository;

    private ObjectMapper objectMapper;
    private RoadmapContentResolver resolver;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AdaptiveRoadmapProperties properties = new AdaptiveRoadmapProperties();
        RoadmapContentSnapshotService snapshots = new RoadmapContentSnapshotService(
                vocabularyRepository, speakingScenarioRepository, ipaPhonemeRepository, properties);
        resolver = new RoadmapContentResolver(objectMapper, onboardingRepository, snapshots);
    }

    @Test
    void legacyRoadmapIsSnapshottedAndIpaIsSplitAcrossAvailableWeeks() throws Exception {
        RoadmapResponse legacy = RoadmapResponse.builder()
                .cefrLevel("A1")
                .totalWeeks(2)
                .milestones(List.of(
                        RoadmapMilestone.builder().weekNumber(1).title("Tuần 1").modules(List.of(
                                RoadmapModule.builder()
                                        .type("VOCABULARY").title("Từ vựng")
                                        .topicId((short) 3).itemCount(99).cefrLevel("A1").build(),
                                RoadmapModule.builder()
                                        .type("IPA_PRONUNCIATION").title("IPA cũ")
                                        .itemCount(44).cefrLevel("A1").build()
                        )).build(),
                        RoadmapMilestone.builder().weekNumber(2).title("Tuần 2").modules(List.of(
                                RoadmapModule.builder()
                                        .type("SPEAKING").title("Nói")
                                        .topicId((short) 4).itemCount(10).cefrLevel("A1").build()
                        )).build()))
                .build();
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .placementCefrLevel(CefrLevel.A1)
                .roadmapJson(objectMapper.writeValueAsString(legacy))
                .build();
        when(vocabularyRepository.findPublishedIdsByTopicId((short) 3)).thenReturn(List.of(10L, 11L));
        when(speakingScenarioRepository.findActiveIdsForRoadmap(CefrLevel.A1, (short) 4))
                .thenReturn(List.of((short) 20));
        when(ipaPhonemeRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                phoneme(1, PhonemeType.VOWEL_MONO),
                phoneme(2, PhonemeType.VOWEL_DIPH),
                phoneme(3, PhonemeType.CONSONANT),
                phoneme(4, PhonemeType.CONSONANT)));

        RoadmapResponse resolved = resolver.resolve(onboarding);

        assertThat(resolved.getMilestones().get(0).getModules())
                .extracting(RoadmapModule::getModuleKey)
                .containsExactly("VOCABULARY:3", "IPA:IPA_VOWELS_BASIC");
        assertThat(resolved.getMilestones().get(1).getModules())
                .extracting(RoadmapModule::getModuleKey)
                .containsExactly(
                        "SPEAKING:4",
                        "IPA:IPA_DIPHTHONGS",
                        "IPA:IPA_CONSONANTS_1",
                        "IPA:IPA_CONSONANTS_2");
        assertThat(resolved.getMilestones()).flatExtracting(RoadmapMilestone::getModules)
                .allSatisfy(module -> {
                    assertThat(module.getContentItemIds()).isNotNull();
                    assertThat(module.getContentVersion()).hasSize(32);
                });
        verify(onboardingRepository).save(onboarding);

        RoadmapResponse persisted = objectMapper.readValue(onboarding.getRoadmapJson(), RoadmapResponse.class);
        assertThat(persisted.getMilestones()).flatExtracting(RoadmapMilestone::getModules)
                .noneMatch(module -> module.getModuleKey() == null);
    }

    @Test
    void alreadySnapshottedRoadmapIsReadWithoutRewritingJson() throws Exception {
        RoadmapModule module = RoadmapModule.builder()
                .type("VOCABULARY")
                .title("Từ vựng")
                .topicId((short) 3)
                .moduleKey("VOCABULARY:3")
                .contentItemIds(List.of(10L, 11L))
                .contentVersion("0123456789abcdef0123456789abcdef")
                .itemCount(2)
                .build();
        RoadmapResponse current = RoadmapResponse.builder()
                .cefrLevel("A1")
                .totalWeeks(1)
                .milestones(List.of(RoadmapMilestone.builder()
                        .weekNumber(1).modules(List.of(module)).build()))
                .build();
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapJson(objectMapper.writeValueAsString(current))
                .build();

        RoadmapResponse resolved = resolver.resolve(onboarding);

        assertThat(resolved.getMilestones().getFirst().getModules().getFirst().getModuleKey())
                .isEqualTo("VOCABULARY:3");
        verify(onboardingRepository, never()).save(onboarding);
    }

    private IpaPhoneme phoneme(int id, PhonemeType type) {
        return IpaPhoneme.builder().id((short) id).phonemeType(type).build();
    }
}
