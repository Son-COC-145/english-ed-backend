package com.example.english_app.service;

import com.example.english_app.dto.request.TopicRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.TopicResponse;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.TopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class TopicService {
    private final TopicRepository topicRepository;

    public PageResponse<TopicResponse> filterTopics(String nameSearch, Boolean isActive, Pageable pageable) {
        Page<Topic> topics = topicRepository.filterTopics(nameSearch, isActive, pageable);
        return PageResponse.of(topics.map(this::toResponse));
    }

    public TopicResponse getById(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        return toResponse(topic);
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

        return toResponse(topicRepository.save(topic));
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

        return toResponse(topicRepository.save(topic));
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
        return toResponse(topicRepository.save(topic));
    }

    public TopicResponse deactivate(Short id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> ErrorCode.TOPIC_NOT_FOUND.toException());

        topic.setIsActive(false);
        return toResponse(topicRepository.save(topic));
    }

    private TopicResponse toResponse(Topic topic) {
        return TopicResponse.builder()
                .id(topic.getId())
                .nameEn(topic.getNameEn())
                .nameVi(topic.getNameVi())
                .iconUrl(topic.getIconUrl())
                .isActive(topic.getIsActive())
                .build();
    }
}
