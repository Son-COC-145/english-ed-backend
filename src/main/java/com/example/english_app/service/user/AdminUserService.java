package com.example.english_app.service.user;

import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.english_app.dto.request.UserCreateRequest;
import com.example.english_app.dto.request.UserUpdateRequest;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PageResponse<UserResponse> getUsers(String keyword, Role role, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(keyword, role, pageable);

        return PageResponse.<UserResponse>builder()
                .content(userPage.getContent().stream().map(this::toUserResponse).collect(Collectors.toList()))
                .pageSize(userPage.getSize())
                .currentPage(userPage.getNumber() + 1)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ErrorCode.EMAIL_ALREADY_EXISTS.toException();
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(true)
                .build();

        return toUserResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null)
            user.setAvatarUrl(request.getAvatarUrl());
        if (request.getRole() != null)
            user.setRole(request.getRole());
        if (request.getIsActive() != null)
            user.setIsActive(request.getIsActive());

        return toUserResponse(userRepository.save(user));
    }

    public UserResponse activate(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        user.setIsActive(true);

        return toUserResponse(userRepository.save(user));
    }

    public UserResponse deactivate(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        user.setIsActive(false);

        return toUserResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        // Đổi thành Soft Delete để tránh lỗi Foreign Key Constraint (người dùng đã có dữ liệu học tập)
        user.setIsActive(false);
        userRepository.save(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .avatarUrl(user.getAvatarUrl())
                .provider(user.getProvider() != null ? user.getProvider().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}