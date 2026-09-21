package com.example.english_app.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.ImageUploadResponse;
import com.example.english_app.service.storage.ImageUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Image Upload", description = "Dịch vụ tải ảnh dùng chung cho hệ thống")
public class ImageUploadController {
    private final ImageUploadService imageUploadService;

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tải ảnh lên", description = "Nhận multipart field `file`, chỉ hỗ trợ ảnh tối đa 5 MB.")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestPart("file") MultipartFile file) {
        String url = imageUploadService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.success(new ImageUploadResponse(url)));
    }
}
