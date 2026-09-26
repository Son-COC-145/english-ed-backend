package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.service.audio.AzureTtsService;
import com.example.english_app.service.storage.AzureBlobStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementListeningMediaServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Mock private AzureTtsService ttsService;
    @Mock private AzureBlobStorageService blobStorageService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private PlacementListeningMediaService service;

    @Test
    void generatesLongLivedAudioFromInternalTranscript() {
        Question question = Question.builder()
                .id(42L)
                .skill(Skill.LISTENING)
                .questionType(QuestionType.LISTENING)
                .contentJson("{\"transcript\":\"Welcome to the station\"}")
                .build();
        byte[] mp3 = new byte[]{1, 2, 3};
        when(questionRepository.findBySkillAndQuestionType(Skill.LISTENING, QuestionType.LISTENING))
                .thenReturn(List.of(question));
        when(ttsService.synthesizeWord("Welcome to the station", AzureTtsService.VOICE_FEMALE_US))
                .thenReturn(mp3);
        when(blobStorageService.uploadAudio(anyString(), org.mockito.ArgumentMatchers.same(mp3),
                org.mockito.ArgumentMatchers.eq("audio/mpeg")))
                .thenReturn("https://cdn.example/opaque/audio.mp3");

        Map<String, Object> report = service.generateMissingAudio();

        assertThat(report.get("generated")).isEqualTo(1);
        assertThat(question.getPlacementAudioUrl()).isEqualTo("https://cdn.example/opaque/audio.mp3");
        verify(questionRepository).save(question);
    }
}
