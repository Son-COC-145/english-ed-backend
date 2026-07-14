package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String password;
    private String phone;
    private String fullName;
    private String role;
    private String avatarUrl;
    private String provider;
    private String locale;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

