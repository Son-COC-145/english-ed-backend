package com.example.english_app.service.classroom;

import java.io.IOException;
import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private static final Duration CONTENT_REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "video/mp4",
            "audio/mpeg",
            "audio/mp3",
            "image/jpeg",
            "image/png",
            "image/webp");

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
        UploadedFile uploaded;
        try {
            uploaded = cloudinaryService.uploadWithMetadata(file.getBytes(), resourceType, "materials", file.getOriginalFilename());
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

    /**
     * Replaces the stored file of a material (optionally renaming it). The new file is validated and uploaded
     * first; the old Cloudinary object is queued for cleanup in the same transaction, so a failed save keeps it.
     */
    @Transactional
    public TeachingMaterialResponse replaceMaterialFile(Long materialId, Long teacherId, Long courseId,
            MultipartFile file, String title) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());
        if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
            throw ErrorCode.MATERIAL_NOT_FOUND.toException();
        }
        courseAccessService.requireTeacherOrAdmin(teacherId, material.getCourse());

        validateUpload(file);
        if (title != null && !title.isBlank()) {
            validateTitle(title);
        }
        contentValidator.validate(file);

        String resourceType = determineResourceType(file.getContentType());
        UploadedFile uploaded;
        try {
            uploaded = cloudinaryService.uploadWithMetadata(file.getBytes(), resourceType, "materials", file.getOriginalFilename());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file bytes", e);
        }

        String oldUrl = material.getFileUrl();
        String oldPublicId = material.getCloudinaryPublicId();
        String oldResourceType = material.getCloudinaryResourceType();

        material.setFileUrl(uploaded.url());
        material.setCloudinaryPublicId(uploaded.publicId());
        material.setCloudinaryResourceType(uploaded.resourceType());
        material.setFileType(determineFileTypeEnum(file.getContentType()));
        material.setFileSizeKb((int) ((file.getSize() + 1023) / 1024));
        if (title != null && !title.isBlank()) {
            material.setTitle(title.trim());
        }
        TeachingMaterial saved = teachingMaterialRepository.save(material);
        storedFileStore.queueMaterialDeletion(material.getId(), oldUrl, oldPublicId, oldResourceType);
        return classroomMapper.toTeachingMaterialResponse(saved);
    }

    private String determineResourceType(String contentType) {
        if (contentType != null && contentType.startsWith("video/"))
            return "video";
        // Cloudinary serves PDFs uploaded as images inline in the browser.
        if ("application/pdf".equals(contentType))
            return "image";
        // Cloudinary's video resource type also supports audio playback/delivery.
        if (contentType != null && contentType.startsWith("audio/"))
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

    /** Serves legacy Cloudinary raw files through an authorized inline/download response. */
    public MaterialContent getMaterialContent(Long actorId, Long courseId, Long materialId) {
        TeachingMaterial material = teachingMaterialRepository.findById(materialId)
                .orElseThrow(() -> ErrorCode.MATERIAL_NOT_FOUND.toException());
        if (material.getCourse() == null || !material.getCourse().getId().equals(courseId)) {
            throw ErrorCode.MATERIAL_NOT_FOUND.toException();
        }
        courseAccessService.requireCourseViewer(actorId, material.getCourse());
        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(HttpRequest.newBuilder(URI.create(material.getFileUrl()))
                    .timeout(CONTENT_REQUEST_TIMEOUT)
                    .GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw contentUnavailable(material, exception.toString());
        } catch (IOException | IllegalArgumentException exception) {
            throw contentUnavailable(material, exception.toString());
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw contentUnavailable(material, "HTTP " + response.statusCode());
        }
        String remoteContentType = response.headers().firstValue("Content-Type")
                .map(value -> value.split(";", 2)[0]).orElse(null);
        String contentType = mimeType(material.getFileType(), remoteContentType);
        return new MaterialContent(response.body(), contentType, downloadName(material, contentType));
    }

    /** Storage failures are not server bugs: log the cause once and return a mapped 502 instead of a 500 stack trace. */
    private RuntimeException contentUnavailable(TeachingMaterial material, String cause) {
        log.warn("Could not fetch teaching material content: materialId={}, host={}, cause={}",
                material.getId(), hostOf(material.getFileUrl()), cause);
        return ErrorCode.MATERIAL_CONTENT_UNAVAILABLE.toException();
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException exception) {
            return "invalid-url";
        }
    }

    public record MaterialContent(byte[] bytes, String contentType, String filename) {}

    private String mimeType(FileType type, String remoteContentType) {
        if (remoteContentType != null && remoteContentType.startsWith("image/")) return remoteContentType;
        return switch (type) {
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case PPTX -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case MP4 -> "video/mp4";
            case MP3 -> "audio/mpeg";
            default -> "application/octet-stream";
        };
    }

    private String downloadName(TeachingMaterial material, String contentType) {
        String extension = switch (material.getFileType()) {
            case PDF -> ".pdf"; case DOCX -> ".docx"; case PPTX -> ".pptx";
            case MP4 -> ".mp4"; case MP3 -> ".mp3";
            default -> imageExtension(contentType);
        };
        return "/files/" + material.getId() + extension;
    }

    private String imageExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".file";
        };
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
