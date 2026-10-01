package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WebAccountDeletionConfirmation {

    @NotBlank(message = "Verification token is required")
    private String token;
}
