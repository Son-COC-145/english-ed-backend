package com.example.english_app.controller;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.service.ipa.IpaMediaGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/ipa/media")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin IPA Media", description = "Quản lý và tự động tạo file âm thanh IPA qua Azure")
public class AdminIpaMediaController {

    private final IpaMediaGeneratorService ipaMediaGeneratorService;

    @Operation(summary = "Tự động sinh âm thanh mẫu bằng Azure TTS và lưu lên Azure Blob Storage")
    @PostMapping("/auto-generate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> autoGenerateAudio(
            @RequestParam(defaultValue = "false") boolean overwriteAll) {
        Map<String, Object> result = ipaMediaGeneratorService.generateAllIpaAudio(overwriteAll);
        return ResponseEntity.ok(ApiResponse.success(result, (String) result.get("message")));
    }
}
