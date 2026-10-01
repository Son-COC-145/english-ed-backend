package com.example.english_app.controller;

import com.example.english_app.dto.request.WebAccountDeletionConfirmation;
import com.example.english_app.dto.request.WebAccountDeletionRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.service.user.WebAccountDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/account-deletion")
@RequiredArgsConstructor
@Tag(name = "Public account deletion")
public class PublicAccountDeletionController {

    private final WebAccountDeletionService service;

    @PostMapping("/request")
    @Operation(summary = "Send a one-time account-deletion verification link")
    public ResponseEntity<ApiResponse<Void>> request(
            @Valid @RequestBody WebAccountDeletionRequest request) {
        service.requestVerification(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(
                        "Nếu tài khoản tồn tại, email xác minh sẽ được gửi trong ít phút."));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm a web account-deletion request")
    public ResponseEntity<ApiResponse<Void>> confirm(
            @Valid @RequestBody WebAccountDeletionConfirmation request) {
        service.confirm(request.getToken());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Yêu cầu xóa tài khoản đã được xác nhận."));
    }
}
