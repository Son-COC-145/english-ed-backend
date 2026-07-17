package com.example.english_app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSettingsRequest {

    @NotNull(message = "Mục tiêu XP không được để trống")
    private Short dailyGoalXp;

    private LocalTime reminderTime;
}
