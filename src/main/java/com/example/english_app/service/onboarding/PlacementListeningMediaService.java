package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.service.audio.AzureTtsService;
import com.example.english_app.service.storage.AzureBlobStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pre-generates immutable listening media; placement requests never synthesize TTS on demand. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementListeningMediaService {

    private final QuestionRepository questionRepository;
    private final AzureTtsService ttsService;
    private final AzureBlobStorageService blobStorageService;
    private final ObjectMapper objectMapper;

    public Map<String, Object> generateMissingAudio() {
        List<Question> questions = questionRepository.findAll();

        int generated = 0;
        int skipped = 0;
        Map<Long, String> failures = new LinkedHashMap<>();

        for (Question question : questions) {
            if (question.getQuestionType() != QuestionType.LISTENING && question.getQuestionType() != QuestionType.PRONUNCIATION) {
                continue;
            }
            if (question.getPlacementAudioUrl() != null && !question.getPlacementAudioUrl().isBlank() && !question.getPlacementAudioUrl().contains("soundhelix")) {
                skipped++;
                continue;
            }

            try {
                String transcript = extractTranscript(question);
                String voice = question.getId() != null && question.getId() % 2 == 0
                        ? AzureTtsService.VOICE_FEMALE_US
                        : AzureTtsService.VOICE_MALE_US;
                byte[] audio = ttsService.synthesizeWord(transcript, voice);
                String blobPath = "audio/placement/q_" + question.getId() + ".mp3";
                String url = blobStorageService.uploadAudio(blobPath, audio, "audio/mpeg");

                question.setPlacementAudioUrl(url);
                questionRepository.save(question);
                generated++;
            } catch (Exception exception) {
                String reason = exception.getMessage() != null
                        ? exception.getMessage()
                        : exception.getClass().getSimpleName();
                failures.put(question.getId(), reason);
                log.error("Failed to generate placement listening audio for question {}",
                        question.getId(), exception);
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("total", generated + skipped + failures.size());
        report.put("generated", generated);
        report.put("skipped", skipped);
        report.put("failed", failures.size());
        report.put("failures", failures);
        return report;
    }

    private String extractTranscript(Question question) throws Exception {
        JsonNode root = objectMapper.readTree(question.getContentJson());
        if (question.getQuestionType() == QuestionType.LISTENING) {
            JsonNode transcript = root.get("transcript");
            if (transcript != null && !transcript.asText().isBlank()) {
                return transcript.asText().trim();
            }
            if (question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()) {
                return question.getCorrectAnswer().trim();
            }
            throw new IllegalStateException("Listening question is missing its internal transcript and correct answer");
        } else {
            JsonNode word = root.get("word");
            if (word != null && !word.asText().isBlank()) {
                return word.asText().trim();
            }
            if (question.getCorrectAnswer() != null && !question.getCorrectAnswer().isBlank()) {
                return question.getCorrectAnswer().trim();
            }
            throw new IllegalStateException("Pronunciation question is missing its word and correct answer");
        }
    }
}
