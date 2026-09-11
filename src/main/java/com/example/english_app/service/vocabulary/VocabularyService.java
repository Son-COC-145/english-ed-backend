package com.example.english_app.service.vocabulary;

import com.example.english_app.dto.request.VocabularyRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final StudentVocabularyProgressRepository progressRepository;
    private final ObjectMapper objectMapper;

    public VocabularyResponse getById(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());
        Long studentId = resolveCurrentStudentId();
        if (studentId != null && (vocabulary.getStatus() != VocabularyStatus.PUBLISHED
                || vocabulary.getTopic() == null
                || !Boolean.TRUE.equals(vocabulary.getTopic().getIsActive()))) {
            throw ErrorCode.VOCABULARY_NOT_FOUND.toException();
        }
        return toResponse(vocabulary, studentId);
    }

    public PageResponse<VocabularyResponse> filterVocabularies(
            Short topicId,
            Long createdById,
            VocabularyStatus status,
            CefrLevel cefrLevel,
            String wordSearch,
            Pageable pageable) {

        // SECURITY: Student chỉ được xem PUBLISHED, bất kể client truyền status gì
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (isStudent) {
            status = VocabularyStatus.PUBLISHED;
        }

        Long studentId = isStudent ? resolveCurrentStudentId() : null;

        Page<Vocabulary> vocabularies = vocabularyRepository.filterVocabularies(
                topicId, createdById, status, cefrLevel, isStudent, wordSearch, pageable);

        return PageResponse.of(vocabularies.map(v -> toResponse(v, studentId)));
    }

    public VocabularyResponse create(VocabularyRequest request) {
        if (vocabularyRepository.existsByWord(request.getWord())) {
            throw ErrorCode.VOCABULARY_ALREADY_EXISTS.toException();
        }

        User createdBy = null;
        if (request.getCreatedById() != null) {
            createdBy = userRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        }
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        Vocabulary vocabulary = Vocabulary.builder()
                .word(request.getWord())
                .topic(topic)
                .ipaTranscription(request.getIpaTranscription())
                .cefrLevel(request.getCefrLevel())
                .definitionVi(request.getDefinitionVi())
                .imageUrl(request.getImageUrl())
                .audioUkUrl(request.getAudioUkUrl())
                .audioUsUrl(request.getAudioUsUrl())
                .collocationJson(request.getCollocationJson())
                .nuanceNote(request.getNuanceNote())
                .exampleSentencesJson(request.getExampleSentencesJson())
                .dialogueJson(request.getDialogueJson())
                .status(request.getStatus())
                .createdBy(createdBy)
                .publishedAt(request.getPublishedAt())
                .build();

        return toResponse(vocabularyRepository.save(vocabulary), null);
    }

    public VocabularyResponse update(Long id, VocabularyRequest request) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());

        boolean isWordChanged = !vocabulary.getWord().equals(request.getWord());

        if (isWordChanged && vocabularyRepository.existsByWord(request.getWord())) {
            throw ErrorCode.VOCABULARY_ALREADY_EXISTS.toException();
        }

        User createdBy = vocabulary.getCreatedBy(); // Giữ nguyên người tạo nếu không truyền lên
        if (request.getCreatedById() != null) {
            createdBy = userRepository.findById(request.getCreatedById())
                    .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        }
        Topic topic = topicRepository.findById(request.getTopicId())
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        vocabulary.setTopic(topic);
        vocabulary.setWord(request.getWord());
        vocabulary.setIpaTranscription(request.getIpaTranscription());
        vocabulary.setCefrLevel(request.getCefrLevel());
        vocabulary.setDefinitionVi(request.getDefinitionVi());
        vocabulary.setImageUrl(request.getImageUrl());
        vocabulary.setAudioUkUrl(request.getAudioUkUrl());
        vocabulary.setAudioUsUrl(request.getAudioUsUrl());
        vocabulary.setCollocationJson(request.getCollocationJson());
        vocabulary.setNuanceNote(request.getNuanceNote());
        vocabulary.setExampleSentencesJson(request.getExampleSentencesJson());
        vocabulary.setDialogueJson(request.getDialogueJson());
        vocabulary.setStatus(request.getStatus());
        vocabulary.setCreatedBy(createdBy);
        vocabulary.setPublishedAt(request.getPublishedAt());

        return toResponse(vocabularyRepository.save(vocabulary), null);
    }

    public void delete(Long id) {
        Vocabulary vocabulary = vocabularyRepository.findById(id)
                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());
        vocabularyRepository.delete(vocabulary);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Lấy ID của student hiện tại từ SecurityContext.
     * Trả null nếu chưa đăng nhập hoặc không phải student.
     */
    private Long resolveCurrentStudentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        try {
            String email = auth.getName();
            return userRepository.findByEmail(email).map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private List<VocabularyResponse.ExampleSentence> parseExampleSentences(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<VocabularyResponse.ExampleSentence>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<VocabularyResponse.Collocation> parseCollocations(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<VocabularyResponse.Collocation>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<VocabularyResponse.DialogueLine> parseDialogue(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<VocabularyResponse.DialogueLine>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Map Vocabulary entity sang VocabularyResponse.
     * @param studentId null = không populate userProgress (admin/anonymous context)
     */
    public VocabularyResponse toResponse(Vocabulary vocabulary, Long studentId) {
        VocabularyResponse.TopicBrief topicResponse = vocabulary.getTopic() != null
                ? VocabularyResponse.TopicBrief.builder()
                        .id(vocabulary.getTopic().getId())
                        .nameEn(vocabulary.getTopic().getNameEn())
                        .nameVi(vocabulary.getTopic().getNameVi())
                        .build()
                : null;

        VocabularyResponse.UserBrief userResponse = vocabulary.getCreatedBy() != null
                ? VocabularyResponse.UserBrief.builder()
                        .id(vocabulary.getCreatedBy().getId())
                        .email(vocabulary.getCreatedBy().getEmail())
                        .fullName(vocabulary.getCreatedBy().getFullName())
                        .build()
                : null;

        // Populate userProgress nếu là student request
        VocabularyResponse.UserProgressBrief progressBrief = null;
        if (studentId != null) {
            Optional<StudentVocabularyProgress> progress = progressRepository
                    .findByStudentIdAndVocabularyId(studentId, vocabulary.getId());
            if (progress.isPresent()) {
                StudentVocabularyProgress p = progress.get();
                progressBrief = VocabularyResponse.UserProgressBrief.builder()
                        .status(p.getStatus())
                        .lastReviewedAt(p.getLastPracticedAt())
                        .nextReviewAt(p.getNextReviewAt())
                        .build();
            }
        }

        return VocabularyResponse.builder()
                .id(vocabulary.getId())
                .topic(topicResponse)
                .word(vocabulary.getWord())
                .ipaTranscription(vocabulary.getIpaTranscription())
                .cefrLevel(vocabulary.getCefrLevel() != null ? vocabulary.getCefrLevel().name() : null)
                .definitionVi(vocabulary.getDefinitionVi())
                .imageUrl(vocabulary.getImageUrl())
                .audioUkUrl(vocabulary.getAudioUkUrl())
                .audioUsUrl(vocabulary.getAudioUsUrl())
                .nuanceNote(vocabulary.getNuanceNote())
                .exampleSentences(parseExampleSentences(vocabulary.getExampleSentencesJson()))
                .collocations(parseCollocations(vocabulary.getCollocationJson()))
                .dialogue(parseDialogue(vocabulary.getDialogueJson()))
                .status(vocabulary.getStatus() != null ? vocabulary.getStatus().name() : null)
                .createdBy(userResponse)
                .publishedAt(vocabulary.getPublishedAt())
                .createdAt(vocabulary.getCreatedAt())
                .userProgress(progressBrief)
                .build();
    }
}
