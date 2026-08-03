package com.example.english_app.service.vocabulary;

import com.example.english_app.dto.request.VocabularyRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VocabularyService {
        private final VocabularyRepository vocabularyRepository;
        private final TopicRepository topicRepository;
        private final UserRepository userRepository;

        public VocabularyResponse getById(Long id) {
                Vocabulary vocabulary = vocabularyRepository.findById(id)
                                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());
                return toResponse(vocabulary);
        }

        public PageResponse<VocabularyResponse> filterVocabularies(
                        Short topicId,
                        Long createdById,
                        VocabularyStatus status,
                        CefrLevel cefrLevel,
                        String wordSearch,
                        Pageable pageable) {

            Page<Vocabulary> vocabularies = vocabularyRepository.filterVocabularies(
                    topicId, createdById, status, cefrLevel, wordSearch, pageable);

            return PageResponse.of(vocabularies.map(this::toResponse));
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

                return toResponse(vocabularyRepository.save(vocabulary));
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

                return toResponse(vocabularyRepository.save(vocabulary));
        }

        public void delete(Long id) {
                Vocabulary vocabulary = vocabularyRepository.findById(id)
                                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());
                vocabularyRepository.delete(vocabulary);
        }

        private VocabularyResponse toResponse(Vocabulary vocabulary) {
                VocabularyResponse.TopicBrief topicResponse = vocabulary.getTopic() != null ? VocabularyResponse.TopicBrief.builder()
                        .id(vocabulary.getTopic().getId())
                        .nameEn(vocabulary.getTopic().getNameEn())
                        .nameVi(vocabulary.getTopic().getNameVi())
                        .build() : null;

                VocabularyResponse.UserBrief userResponse = vocabulary.getCreatedBy() != null ? VocabularyResponse.UserBrief.builder()
                        .id(vocabulary.getCreatedBy().getId())
                        .email(vocabulary.getCreatedBy().getEmail())
                        .fullName(vocabulary.getCreatedBy().getFullName())
                        .build() : null;

                return VocabularyResponse.builder()
                                .id(vocabulary.getId())
                                .topic(topicResponse)
                                .word(vocabulary.getWord())
                                .ipaTranscription(vocabulary.getIpaTranscription())
                                .cefrLevel(vocabulary.getCefrLevel().name())
                                .definitionVi(vocabulary.getDefinitionVi())
                                .imageUrl(vocabulary.getImageUrl())
                                .audioUkUrl(vocabulary.getAudioUkUrl())
                                .audioUsUrl(vocabulary.getAudioUsUrl())
                                .collocationJson(vocabulary.getCollocationJson())
                                .nuanceNote(vocabulary.getNuanceNote())
                                .exampleSentencesJson(vocabulary.getExampleSentencesJson())
                                .dialogueJson(vocabulary.getDialogueJson())
                                .status(vocabulary.getStatus().name())
                                .createdBy(userResponse)
                                .publishedAt(vocabulary.getPublishedAt())
                                .createdAt(vocabulary.getCreatedAt())
                                .build();
        }
}
