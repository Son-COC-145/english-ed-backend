package com.example.english_app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSettingsRequest {

    @NotNull(message = "Mục tiêu XP không được để trống")
    private Short dailyGoalXp;

    @Schema(type = "string", example = "20:30:00", description = "Thời gian nhắc nhở học hàng ngày (định dạng HH:mm:ss)")
    private LocalTime reminderTime;
}
