package com.example.english_app.service.vocabulary;

import com.example.english_app.dto.request.TopicRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.TopicResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final VocabularyRepository vocabularyRepository;
    private final StudentVocabularyProgressRepository progressRepository;
    private final UserRepository userRepository;

    public PageResponse<TopicResponse> filterTopics(String nameSearch, Boolean isActive, Pageable pageable) {
        if (isCurrentStudent()) isActive = Boolean.TRUE;
        Page<Topic> topics = topicRepository.filterTopics(nameSearch, isActive, pageable);
        List<Topic> topicList = topics.getContent();

        if (topicList.isEmpty()) {
            return PageResponse.of(topics.map(t -> toResponse(t, 0L, null)));
        }

        List<Short> topicIds = topicList.stream().map(Topic::getId).toList();
        boolean student = isCurrentStudent();
        Long studentId = student ? resolveCurrentStudentId() : null;

        // 1. Batch query vocabulary count
        Map<Short, Long> vocabCountMap = new HashMap<>();
        List<Object[]> vocabCounts = vocabularyRepository.countVocabulariesByTopicIds(topicIds, student);
        for (Object[] row : vocabCounts) {
            Short tId = (Short) row[0];
            Long count = (Long) row[1];
            vocabCountMap.put(tId, count);
        }

        // 2. Batch query mastered count (nếu là student)
        Map<Short, Long> masteredCountMap = new HashMap<>();
        if (studentId != null) {
            List<Object[]> masteredCounts = progressRepository.countMasteredByStudentIdAndTopicIds(studentId, topicIds);
            for (Object[] row : masteredCounts) {
                Short tId = (Short) row[0];
                Long count = (Long) row[1];
                masteredCountMap.put(tId, count);
            }
        }

        List<TopicResponse> responses = topicList.stream().map(topic -> {
            Long vCount = vocabCountMap.getOrDefault(topic.getId(), 0L);
            Long mCount = studentId != null ? masteredCountMap.getOrDefault(topic.getId(), 0L) : null;
            return toResponse(topic, vCount, mCount);
        }).toList();

        return PageResponse.<TopicResponse>builder()
                .content(responses)
                .currentPage(topics.getNumber())
                .pageSize(topics.getSize())
                .totalElements(topics.getTotalElements())
                .totalPages(topics.getTotalPages())
                .build();
    }

    public TopicResponse getById(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());
        if (isCurrentStudent() && !Boolean.TRUE.equals(topic.getIsActive())) {
            throw ErrorCode.TOPIC_NOT_FOUND.toException();
        }

        boolean student = isCurrentStudent();
        Long studentId = student ? resolveCurrentStudentId() : null;

        long vCount = vocabularyRepository.countVocabulariesByTopicId(id, student);
        Long mCount = studentId != null ? progressRepository.countMasteredByStudentIdAndTopicId(studentId, id) : null;

        return toResponse(topic, vCount, mCount);
    }

    public TopicResponse create(TopicRequest request) {
        if (topicRepository.existsByNameEn(request.getNameEn())
                || topicRepository.existsByNameVi(request.getNameVi())) {
            throw ErrorCode.TOPIC_ALREADY_EXISTS.toException();
        }
        Topic topic = new Topic();
        topic.setNameEn(request.getNameEn());
        topic.setNameVi(request.getNameVi());
        topic.setIconUrl(request.getIconUrl());
        topic.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return toResponse(topicRepository.save(topic), 0L, null);
    }

    public TopicResponse update(Short topicId, TopicRequest request) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        boolean isNameEnChanged = !topic.getNameEn().equals(request.getNameEn());
        boolean isNameViChanged = !topic.getNameVi().equals(request.getNameVi());

        if ((isNameEnChanged && topicRepository.existsByNameEn(request.getNameEn()))
                || (isNameViChanged && topicRepository.existsByNameVi(request.getNameVi()))) {
            throw ErrorCode.TOPIC_ALREADY_EXISTS.toException();
        }

        topic.setNameEn(request.getNameEn());
        topic.setNameVi(request.getNameVi());
        topic.setIconUrl(request.getIconUrl());
        topic.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        long vCount = vocabularyRepository.countVocabulariesByTopicId(topicId, false);
        return toResponse(topicRepository.save(topic), vCount, null);
    }

    public void delete(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        topicRepository.delete(topic);
    }

    public TopicResponse activate(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        topic.setIsActive(true);
        long vCount = vocabularyRepository.countVocabulariesByTopicId(id, false);
        return toResponse(topicRepository.save(topic), vCount, null);
    }

    public TopicResponse deactivate(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        topic.setIsActive(false);
        long vCount = vocabularyRepository.countVocabulariesByTopicId(id, false);
        return toResponse(topicRepository.save(topic), vCount, null);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Long resolveCurrentStudentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        boolean isStudent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
        if (!isStudent) return null;
        try {
            String email = auth.getName();
            return userRepository.findByEmail(email).map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isCurrentStudent() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    }

    private TopicResponse toResponse(Topic topic, Long vocabularyCount, Long masteredCount) {
        return TopicResponse.builder()
                .id(topic.getId())
                .nameEn(topic.getNameEn())
                .nameVi(topic.getNameVi())
                .iconUrl(topic.getIconUrl())
                .isActive(topic.getIsActive())
                .vocabularyCount(vocabularyCount)
                .masteredCount(masteredCount)
                .build();
    }
}
