package com.example.english_app.controller;

import com.example.english_app.dto.request.UpdateProfileRequest;
import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
