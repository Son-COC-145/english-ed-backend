package com.example.english_app.mapper;

import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.dto.response.classroom.*;
import com.example.english_app.entity.classroom.*;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;
import com.example.english_app.dto.response.TopicResponse;
import com.example.english_app.entity.vocabulary.Topic;
import com.example.english_app.entity.user.User;

@Component
public class ClassroomMapper {

    public CourseResponse toCourseResponse(Course entity) {
        if (entity == null)
            return null;

        User teacherEntity = entity.getTeacher();
        CourseTeacherResponse teacher = null;
        if (teacherEntity != null) {
            teacher = CourseTeacherResponse.builder()
                    .id(teacherEntity.getId())
                    .fullName(teacherEntity.getFullName())
                    .email(teacherEntity.getEmail())
                    .avatarUrl(teacherEntity.getAvatarUrl())
                    .build();
        }

        return CourseResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .teacher(teacher)
                .description(entity.getDescription())
                .cefrTarget(entity.getCefrTarget())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CourseStudentResponse toCourseStudentResponse(CourseStudent entity) {
        if (entity == null)
            return null;
        return CourseStudentResponse.builder()
                .id(entity.getId())
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .studentId(entity.getStudent() != null ? entity.getStudent().getId() : null)
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TeachingMaterialResponse toTeachingMaterialResponse(TeachingMaterial entity) {
        if (entity == null)
            return null;
        return TeachingMaterialResponse.builder()
                .id(entity.getId())
                .teacherId(entity.getTeacher() != null ? entity.getTeacher().getId() : null)
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .title(entity.getTitle())
                .fileType(entity.getFileType())
                .fileUrl(entity.getFileUrl())
                .fileSizeKb(entity.getFileSizeKb())
                .isLivePresenting(entity.getIsLivePresenting())
                .uploadedAt(entity.getUploadedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AssignmentResponse toAssignmentResponse(Assignment entity) {
        if (entity == null)
            return null;
        return AssignmentResponse.builder()
                .id(entity.getId())
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .teacherId(entity.getTeacher() != null ? entity.getTeacher().getId() : null)
                .title(entity.getTitle())
                .description(entity.getDescription())
                .moduleType(entity.getModuleType())
                .refId(entity.getRefId())
                .deadlineAt(entity.getDeadlineAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AssignmentSubmissionResponse toAssignmentSubmissionResponse(AssignmentSubmission entity) {
        if (entity == null)
            return null;
        User student = entity.getStudent();
        return AssignmentSubmissionResponse.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null)
                .studentId(student != null ? student.getId() : null)
                .studentInfo(student != null ? SubmissionStudentResponse.builder()
                        .id(student.getId())
                        .fullName(student.getFullName())
                        .email(student.getEmail())
                        .avatarUrl(student.getAvatarUrl())
                        .build() : null)
                .resultRefId(entity.getResultRefId())
                .score(entity.getScore())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .teacherCommentText(entity.getTeacherCommentText())
                .teacherAudioCommentUrl(entity.getTeacherAudioCommentUrl())
                .commentedAt(entity.getCommentedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SyllabusItemResponse toSyllabusItemResponse(SyllabusItem entity) {
        if (entity == null)
            return null;
        return SyllabusItemResponse.builder()
                .id(entity.getId())
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .weekNumber(entity.getWeekNumber())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .scheduledDate(entity.getScheduledDate())
                .materialId(entity.getMaterial() != null ? entity.getMaterial().getId() : null)
                .sortOrder(entity.getSortOrder())
                .topics(entity.getTopics() != null ? entity.getTopics().stream().map(this::toTopicResponse).collect(Collectors.toList()) : null)
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TopicResponse toTopicResponse(Topic topic) {
        if (topic == null) return null;
        return TopicResponse.builder()
                .id(topic.getId())
                .nameEn(topic.getNameEn())
                .nameVi(topic.getNameVi())
                .iconUrl(topic.getIconUrl())
                .isActive(topic.getIsActive())
                .build();
    }

    public CourseStudentDetailResponse toCourseStudentDetailResponse(
            CourseStudent entity,
            UserResponse studentInfo,
            StudentStatResponse studentProgress) {

        if (entity == null)
            return null;
        return CourseStudentDetailResponse.builder()
                .id(entity.getId())
                .courseId(entity.getCourse() != null ? entity.getCourse().getId() : null)
                .status(entity.getStatus())
                .joinedAt(entity.getJoinedAt())
                .studentInfo(studentInfo)
                .studentProgress(studentProgress)
                .build();
    }
}
