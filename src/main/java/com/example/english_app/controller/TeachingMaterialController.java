package com.example.english_app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.dto.request.classroom.UpdateTeachingMaterialRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.classroom.TeachingMaterialResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.classroom.TeachingMaterialService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/materials")
@RequiredArgsConstructor
@Tag(name = "TeachingMaterial", description = "TeachingMaterial API")
public class TeachingMaterialController {
    private final TeachingMaterialService teachingMaterialService;
    private final UserRepository userRepository;

    private User getCurrentTeacher() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    @Operation(summary = "Lấy danh sách tài liệu của lớp")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TeachingMaterialResponse>>> getMaterials(@PathVariable Long courseId) {
        return ResponseEntity.ok(
                ApiResponse.success(teachingMaterialService.getMaterialsByCourse(getCurrentTeacher().getId(), courseId)));
    }

    @Operation(summary = "Upload tài liệu giảng dạy cho lớp học")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeachingMaterialResponse>> uploadeMaterial(
            @PathVariable Long courseId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title) {

        User teacher = getCurrentTeacher();

        return ResponseEntity.ok(ApiResponse.success(
                teachingMaterialService.uploadAndCreateMaterial(teacher.getId(), courseId, file, title)));
    }

    @Operation(summary = "Xóa tài liệu")
    @DeleteMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMaterial(
            @PathVariable Long courseId,
            @PathVariable Long materialId) {
        User teacher = getCurrentTeacher();
        teachingMaterialService.deleteMaterial(courseId, materialId, teacher.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Cập nhật tài liệu")
    @PutMapping("/{materialId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeachingMaterialResponse>> updateMaterial(
            @PathVariable Long courseId,
            @PathVariable Long materialId,
            @RequestBody UpdateTeachingMaterialRequest request) {
        User teacher = getCurrentTeacher();
        TeachingMaterialResponse response = teachingMaterialService.updateMaterialInfo(materialId, teacher.getId(),
                request.getTitle(), courseId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
