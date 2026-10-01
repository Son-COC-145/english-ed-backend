package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AccountDeletionRequestDto {

    @NotBlank(message = "Confirmation is required")
    @Pattern(regexp = "DELETE", message = "Confirmation must be DELETE")
    private String confirmation;

    private String refreshToken;
}
