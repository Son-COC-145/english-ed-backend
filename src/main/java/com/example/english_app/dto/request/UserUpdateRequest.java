package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.Role;
import lombok.Data;

@Data
public class UserUpdateRequest {
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Role role;
    private Boolean isActive;
}
