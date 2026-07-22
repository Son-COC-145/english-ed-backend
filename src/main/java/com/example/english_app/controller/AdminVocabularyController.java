package com.example.english_app.controller;

import com.example.english_app.dto.response.AiVocabularyResponse;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.example.english_app.service.integration.AiTextService;
import com.example.english_app.service.integration.ImageGenerationService;
import com.example.english_app.service.integration.TtsGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/admin/vocabularies")
@RequiredArgsConstructor
@Tag(name = "Admin Vocabulary", description = "API for managing vocabularies")
public class AdminVocabularyController {

    private final AiTextService aiTextService;
    private final ImageGenerationService imageGenerationService;
    private final TtsGenerationService ttsGenerationService;
    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;

    @Operation(summary = "Generate vocabulary", description = "Generate vocabulary")
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Vocabulary> generateVocabulary(
            @RequestParam String word,
            @RequestParam Short topicId,
            @RequestParam String cefr) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new RuntimeException("Topic not found"));

        try {
            // Chạy 3 task song song
            CompletableFuture<AiVocabularyResponse> textTask = CompletableFuture
                    .supplyAsync(() -> aiTextService.generateVocabularyData(word, topic.getNameEn(), cefr));

            CompletableFuture<String> imageTask = CompletableFuture
                    .supplyAsync(() -> imageGenerationService.generateImage(word, topic.getNameEn()));

            CompletableFuture<String> audioTask = CompletableFuture
                    .supplyAsync(() -> ttsGenerationService.generateAudio(word));

            // Chờ cả 3 task hoàn thành
            CompletableFuture.allOf(textTask, imageTask, audioTask).join();

            AiVocabularyResponse textData = textTask.get();
            String imageUrl = imageTask.get();
            String audioUrl = audioTask.get();

            // Ánh xạ vào Entity
            Vocabulary vocab = Vocabulary.builder()
                    .word(word)
                    .topic(topic)
                    .cefrLevel(com.example.english_app.entity.enums.CefrLevel.valueOf(cefr))
                    .ipaTranscription(textData.getIpaTranscription())
                    .definitionVi(textData.getDefinitionVi())
                    .nuanceNote(textData.getNuanceNote())
                    .exampleSentencesJson(objectMapper.writeValueAsString(textData.getExampleSentencesJson()))
                    .collocationJson(objectMapper.writeValueAsString(textData.getCollocationJson()))
                    .dialogueJson(objectMapper.writeValueAsString(textData.getDialogueJson()))
                    .imageUrl(imageUrl)
                    .audioUsUrl(audioUrl)
                    .status(VocabularyStatus.DRAFT) // Lưu ở trạng thái nháp
                    .build();

            return ResponseEntity.ok(vocabularyRepository.save(vocab));

        } catch (Exception e) {
            throw new RuntimeException("Lỗi sinh từ vựng: " + e.getMessage(), e);
        }
    }
}

