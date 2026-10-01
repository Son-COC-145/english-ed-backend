package com.example.english_app.controller;

import com.example.english_app.dto.request.AccountDeletionRequestDto;
import com.example.english_app.dto.request.UpdateProfileRequest;
import com.example.english_app.dto.response.AccountDeletionResponse;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.service.user.AccountDeletionService;
import com.example.english_app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "Quản lý thông tin người dùng")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;

    @Operation(summary = "Lấy thông tin tài khoản hiện tại")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUser(authentication.getName())));
    }

    @Operation(summary = "Cập nhật thông tin tài khoản cá nhân")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(authentication.getName(), request)));
    }

    @Operation(summary = "Xóa tài khoản cá nhân")
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/me/deletion-request")
    public ResponseEntity<ApiResponse<AccountDeletionResponse>> requestAccountDeletion(
            Authentication authentication,
            @Valid @RequestBody AccountDeletionRequestDto request) {
        AccountDeletionResponse response = accountDeletionService.requestFromApp(
                authentication.getName(), request.getRefreshToken());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Yêu cầu xóa tài khoản đã được tiếp nhận"));
    }

    /** Compatibility endpoint for mobile builds released before the durable workflow. */
    @Deprecated
    @Operation(summary = "Yêu cầu xóa tài khoản (legacy)", deprecated = true)
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccountLegacy(
            Authentication authentication,
            @RequestParam(required = false) String refreshToken) {
        accountDeletionService.requestFromApp(authentication.getName(), refreshToken);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Yêu cầu xóa tài khoản đã được tiếp nhận"));
    }
}
