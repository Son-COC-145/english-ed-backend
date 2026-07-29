package com.example.english_app.service.classroom;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.dto.response.classroom.TeachingMaterialResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.TeachingMaterial;
import com.example.english_app.entity.enums.FileType;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.TeachingMaterialRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.integration.CloudinaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeachingMaterialService {
    private final CourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final ClassroomMapper classroomMapper;
    private final TeachingMaterialRepository teachingMaterialRepository;

    @Transactional
    public TeachingMaterialResponse uploadAndCreateMaterial(Long teacherId, Long courseId, MultipartFile file,
            String title) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        Course course = null;
        if (courseId != null) {
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        }

        String resourceType = determineResourceType(file.getContentType());
        String publicId = "materials/" + UUID.randomUUID().toString();
        String fileUrl;
        try {
            fileUrl = cloudinaryService.uploadFile(file.getBytes(), resourceType, publicId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        FileType fileType = determineFileTypeEnum(file.getContentType());

        Integer fileSizeKb = (int) (file.getSize() / 1024);

        TeachingMaterial material = TeachingMaterial.builder()
                .title(title)
                .fileUrl(fileUrl)
                .fileType(fileType)
                .fileSizeKb(fileSizeKb)
                .course(course)
                .teacher(teacher)
                .isLivePresenting(false)
                .build();

        return classroomMapper.toTeachingMaterialResponse(teachingMaterialRepository.save(material));
    }

    public List<TeachingMaterialResponse> getMaterialsByCourse(Long courseId) {
        return teachingMaterialRepository.findAllByCourseId(courseId)
                .stream()
                .map(classroomMapper::toTeachingMaterialResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMaterial(Long materialId, Long teacherId) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());

        if (!material.getTeacher().getId().equals(teacherId)) {
            throw ErrorCode.UNAUTHORIZED.toException();
        }
        teachingMaterialRepository.delete(material);
    }

    @Transactional
    public TeachingMaterialResponse updateMaterialInfo(Long materialId, Long teacherId, String title, Long courseId) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());

        if (!material.getTeacher().getId().equals(teacherId)) {
            throw ErrorCode.UNAUTHORIZED.toException();
        }

        if (courseId != null) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
            material.setCourse(course);
        } else {
            material.setCourse(null);
        }

        if (title != null && !title.trim().isEmpty()) {
            material.setTitle(title);
        }

        return classroomMapper.toTeachingMaterialResponse(teachingMaterialRepository.save(material));
    }

    private String determineResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("video/"))
            return "video";
        if (contentType != null && contentType.startsWith("image/"))
            return "image";
        return "raw";
    }

    private FileType determineFileTypeEnum(String contentType) {
        if (contentType == null)
            return FileType.OTHER;
        if (contentType.contains("pdf"))
            return FileType.PDF;
        if (contentType.contains("powerpoint") || contentType.contains("presentation"))
            return FileType.PPTX;
        if (contentType.startsWith("video/"))
            return FileType.MP4;
        if (contentType.startsWith("audio/"))
            return FileType.MP3;
        if (contentType.startsWith("image/"))
            return FileType.OTHER;
        return FileType.OTHER;
    }
}
