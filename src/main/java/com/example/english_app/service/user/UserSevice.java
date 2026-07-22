package com.example.english_app.service.user;

import com.example.english_app.dto.response.UserResponse;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSevice {
    private final UserRepository userRepository;

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


