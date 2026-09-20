package com.example.english_app.service.classroom;

import java.util.List;
import java.util.UUID;
import java.util.Set;
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
import com.example.english_app.service.integration.CloudinaryService.UploadedFile;
import com.example.english_app.service.storage.StoredFileStore;
import com.example.english_app.service.storage.FileContentValidator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeachingMaterialService {
    private final CourseRepository courseRepository;
    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final ClassroomMapper classroomMapper;
    private final TeachingMaterialRepository teachingMaterialRepository;
    private final CourseAccessService courseAccessService;
    private final StoredFileStore storedFileStore;
    private final FileContentValidator contentValidator;

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4",
            "audio/mpeg",
            "audio/mp3");

    @Transactional
    public TeachingMaterialResponse uploadAndCreateMaterial(Long teacherId, Long courseId, MultipartFile file,
            String title) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        Course course = null;
        if (courseId != null) {
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
            courseAccessService.requireTeacherOrAdmin(teacherId, course);
        }

        validateUpload(file);
        validateTitle(title);
        contentValidator.validate(file);

        String resourceType = determineResourceType(file.getContentType());
        String publicId = "materials/" + UUID.randomUUID().toString();
        UploadedFile uploaded;
        try {
            uploaded = cloudinaryService.uploadWithMetadata(file.getBytes(), resourceType, publicId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        FileType fileType = determineFileTypeEnum(file.getContentType());

        Integer fileSizeKb = (int) ((file.getSize() + 1023) / 1024);

        TeachingMaterial material = TeachingMaterial.builder()
                .title(title.trim())
                .fileUrl(uploaded.url())
                .cloudinaryPublicId(uploaded.publicId())
                .cloudinaryResourceType(uploaded.resourceType())
                .fileType(fileType)
                .fileSizeKb(fileSizeKb)
                .course(course)
                .teacher(teacher)
                .isLivePresenting(false)
                .build();

        TeachingMaterial savedMaterial = teachingMaterialRepository.save(material);
        return classroomMapper.toTeachingMaterialResponse(savedMaterial);
    }

    public List<TeachingMaterialResponse> getMaterialsByCourse(Long actorId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> ErrorCode.COURSE_NOT_FOUND.toException());
        courseAccessService.requireCourseViewer(actorId, course);
        return teachingMaterialRepository.findAllByCourseId(courseId)
                .stream()
                .map(classroomMapper::toTeachingMaterialResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMaterial(Long courseId, Long materialId, Long teacherId) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());

        if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
            throw ErrorCode.MATERIAL_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, material.getCourse());
        teachingMaterialRepository.delete(material);
        storedFileStore.queueMaterialDeletion(material.getId(), material.getFileUrl(),
                material.getCloudinaryPublicId(), material.getCloudinaryResourceType());
    }

    @Transactional
    public TeachingMaterialResponse updateMaterialInfo(Long materialId, Long teacherId, String title, Long courseId) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());

        if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
            throw ErrorCode.MATERIAL_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, material.getCourse());

        if (title != null) {
            validateTitle(title);
            material.setTitle(title.trim());
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
        if (contentType.contains("wordprocessingml"))
            return FileType.DOCX;
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

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE_BYTES
                || file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty() || title.trim().length() > 300) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
    }
}
