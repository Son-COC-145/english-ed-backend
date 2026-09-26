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
import com.example.english_app.dto.response.AudioUploadResponse;
import com.example.english_app.service.storage.AudioUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
@PreAuthorize("hasRole('TEACHER')")
@Tag(name = "Teacher Audio Upload", description = "Tải audio nhận xét bài nộp của giáo viên")
public class AudioUploadController {
    private final AudioUploadService audioUploadService;

    @PostMapping(value = "/upload-audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Tải audio nhận xét",
            description = "Nhận multipart field `file`: MP3, WAV, WEBM hoặc M4A, tối đa 5 MB. Trả về URL HTTPS.")
    public ResponseEntity<ApiResponse<AudioUploadResponse>> uploadAudio(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(new AudioUploadResponse(audioUploadService.uploadAudio(file))));
    }
}
