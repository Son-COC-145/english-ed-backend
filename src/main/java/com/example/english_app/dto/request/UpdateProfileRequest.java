package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;
    
    private String phone;
    
    private String avatarUrl;
}
