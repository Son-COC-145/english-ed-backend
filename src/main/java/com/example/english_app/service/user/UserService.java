package com.example.english_app.service.user;

import com.example.english_app.dto.request.UpdateProfileRequest;
import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
        return toUserResponse(user);
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());

        return toUserResponse(userRepository.save(user));
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
