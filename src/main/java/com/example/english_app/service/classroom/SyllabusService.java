package com.example.english_app.service.classroom;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.english_app.dto.request.classroom.SyllabusItemRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.classroom.SyllabusItemResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.SyllabusItem;
import com.example.english_app.entity.classroom.TeachingMaterial;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.SyllabusItemRepository;
import com.example.english_app.repository.classroom.TeachingMaterialRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.entity.vocabulary.Topic;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SyllabusService {
    private final SyllabusItemRepository syllabusItemRepository;
    private final CourseRepository courseRepository;
    private final TeachingMaterialRepository teachingMaterialRepository;
    private final TopicRepository topicRepository;
    private final ClassroomMapper classroomMapper;
    private final CourseAccessService courseAccessService;

    @Transactional
    public SyllabusItemResponse createSyllabusItem(Long actorId, Long courseId, SyllabusItemRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireTeacherOrAdmin(actorId, course);

        TeachingMaterial material = null;

        if (request.getMaterialId() != null) {
            material = teachingMaterialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());
            if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
                throw ErrorCode.MATERIAL_NOT_FOUND.toException();
            }
        }

        SyllabusItem item = SyllabusItem.builder()
                .course(course)
                .material(material)
                .weekNumber(request.getWeekNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledDate(request.getScheduledDate())
                .sortOrder(request.getSortOrder())
                .build();

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            List<Topic> topics = topicRepository.findAllById(request.getTopicIds());
            if (topics.size() != request.getTopicIds().stream().distinct().count()) {
                throw ErrorCode.TOPIC_NOT_FOUND.toException();
            }
            item.setTopics(topics);
        }

        return classroomMapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

    public PageResponse<SyllabusItemResponse> getSyllabusByCourseWithFilters(
            Long actorId, Long courseId, String keyword, Short weekNumber, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireCourseViewer(actorId, course);

        Page<SyllabusItem> pageResult = syllabusItemRepository.findAllByCourseIdWithFilters(
                courseId, keyword, weekNumber, pageable);

        List<SyllabusItemResponse> items = pageResult.getContent().stream()
                .map(classroomMapper::toSyllabusItemResponse)
                .collect(Collectors.toList());

        return PageResponse.<SyllabusItemResponse>builder()
                .content(items)
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .build();
    }

    @Transactional
    public void deleteSyllabusItem(Long actorId, Long courseId, Long itemId) {
        SyllabusItem item = syllabusItemRepository.findById(itemId)
                .orElseThrow(() -> ErrorCode.SYLLABUS_NOT_FOUND.toException());
        if (!item.getCourse().getId().equals(courseId)) {
            throw ErrorCode.SYLLABUS_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(actorId, item.getCourse());
        syllabusItemRepository.delete(item);
    }

    @Transactional
    public SyllabusItemResponse updateSyllabusItem(Long actorId, Long courseId, Long itemId, SyllabusItemRequest request) {
        SyllabusItem item = syllabusItemRepository.findById(itemId)
                .orElseThrow(() -> ErrorCode.SYLLABUS_NOT_FOUND.toException());

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        if (!item.getCourse().getId().equals(courseId)) {
            throw ErrorCode.SYLLABUS_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(actorId, course);

        TeachingMaterial material = null;
        if (request.getMaterialId() != null) {
            material = teachingMaterialRepository.findById(request.getMaterialId())
                    .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());
            if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
                throw ErrorCode.MATERIAL_NOT_FOUND.toException();
            }
        }

        item.setMaterial(material);
        item.setWeekNumber(request.getWeekNumber());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setScheduledDate(request.getScheduledDate());
        item.setSortOrder(request.getSortOrder());

        if (request.getTopicIds() != null && !request.getTopicIds().isEmpty()) {
            List<Topic> topics = topicRepository.findAllById(request.getTopicIds());
            if (topics.size() != request.getTopicIds().stream().distinct().count()) {
                throw ErrorCode.TOPIC_NOT_FOUND.toException();
            }
            item.setTopics(topics);
        } else {
            item.setTopics(null);
        }

        return classroomMapper.toSyllabusItemResponse(syllabusItemRepository.save(item));
    }

}
