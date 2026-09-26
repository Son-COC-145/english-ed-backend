package com.example.english_app.service.classroom;

import com.example.english_app.entity.classroom.Course;
import com.example.english_app.entity.classroom.TeachingMaterial;
import com.example.english_app.entity.enums.FileType;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.mapper.ClassroomMapper;
import com.example.english_app.repository.classroom.CourseRepository;
import com.example.english_app.repository.classroom.TeachingMaterialRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.integration.CloudinaryService;
import com.example.english_app.service.storage.FileContentValidator;
import com.example.english_app.service.storage.StoredFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeachingMaterialServiceTest {
    private static final byte[] PDF_BYTES = "%PDF-1.7\n%%EOF".getBytes();

    @Mock private CourseRepository courseRepository;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private TeachingMaterialRepository teachingMaterialRepository;
    @Mock private CourseAccessService courseAccessService;
    @Mock private StoredFileStore storedFileStore;
    @Mock private FileContentValidator contentValidator;
    @InjectMocks private TeachingMaterialService service;

    @Test
    void unreachableStorageHostReturnsMappedErrorInsteadOf500() {
        Course course = Course.builder().id(10L).build();
        // ".invalid" is reserved (RFC 2606) and never resolves, like the seeded cdn.example.com links.
        TeachingMaterial material = TeachingMaterial.builder().id(1L).course(course).title("Slides")
                .fileType(FileType.PDF).fileUrl("https://materials.example.invalid/slides.pdf").build();
        when(teachingMaterialRepository.findById(1L)).thenReturn(Optional.of(material));

        AppException error = assertThrows(AppException.class, () -> service.getMaterialContent(5L, 10L, 1L));

        assertEquals(ErrorCode.MATERIAL_CONTENT_UNAVAILABLE, error.getErrorCode());
        verify(courseAccessService).requireCourseViewer(5L, course);
    }

    @Test
    void malformedStoredUrlReturnsMappedError() {
        Course course = Course.builder().id(10L).build();
        TeachingMaterial material = TeachingMaterial.builder().id(2L).course(course).title("Broken")
                .fileType(FileType.PDF).fileUrl("not a url").build();
        when(teachingMaterialRepository.findById(2L)).thenReturn(Optional.of(material));

        AppException error = assertThrows(AppException.class, () -> service.getMaterialContent(5L, 10L, 2L));

        assertEquals(ErrorCode.MATERIAL_CONTENT_UNAVAILABLE, error.getErrorCode());
    }

    @Test
    void replaceFileSwapsStoredFileAndQueuesOldForCleanup() throws Exception {
        Course course = Course.builder().id(10L).build();
        TeachingMaterial material = TeachingMaterial.builder().id(3L).course(course).title("Old slides")
                .fileType(FileType.PDF).fileUrl("https://cdn.test/old.pdf")
                .cloudinaryPublicId("materials/old").cloudinaryResourceType("image").fileSizeKb(10).build();
        when(teachingMaterialRepository.findById(3L)).thenReturn(Optional.of(material));
        when(cloudinaryService.uploadWithMetadata(any(), eq("image"), eq("materials"), eq("new.pdf")))
                .thenReturn(new CloudinaryService.UploadedFile("https://cdn.test/new.pdf", "materials/new", "image"));
        when(teachingMaterialRepository.save(any(TeachingMaterial.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", PDF_BYTES);

        service.replaceMaterialFile(3L, 5L, 10L, file, "  New slides ");

        verify(courseAccessService).requireTeacherOrAdmin(5L, course);
        assertEquals("https://cdn.test/new.pdf", material.getFileUrl());
        assertEquals("materials/new", material.getCloudinaryPublicId());
        assertEquals("New slides", material.getTitle());
        verify(storedFileStore).queueMaterialDeletion(3L, "https://cdn.test/old.pdf", "materials/old", "image");
    }

    @Test
    void replaceFileRejectsMaterialFromAnotherCourse() {
        TeachingMaterial material = TeachingMaterial.builder().id(3L).course(Course.builder().id(99L).build()).build();
        when(teachingMaterialRepository.findById(3L)).thenReturn(Optional.of(material));
        MockMultipartFile file = new MockMultipartFile("file", "new.pdf", "application/pdf", PDF_BYTES);

        AppException error = assertThrows(AppException.class, () -> service.replaceMaterialFile(3L, 5L, 10L, file, null));

        assertEquals(ErrorCode.MATERIAL_NOT_FOUND, error.getErrorCode());
        verifyNoInteractions(cloudinaryService, storedFileStore);
    }
}
