package com.example.english_app.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.english_app.dto.response.ApiResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.dto.request.UserCreateRequest;
import com.example.english_app.dto.request.UserUpdateRequest;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.service.user.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lấy danh sách user")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.success(adminUserService.getUsers(keyword, role, pageable)));
    }

    @Operation(summary = "Tạo mới user")
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.create(request)));
    }

    @Operation(summary = "Cập nhật user")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.updateUser(id, request)));
    }

    @Operation(summary = "Kích hoạt user")
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.activate(id)));
    }

    @Operation(summary = "Hủy kích hoạt user")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.deactivate(id)));
    }

    @Operation(summary = "Xóa user")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
