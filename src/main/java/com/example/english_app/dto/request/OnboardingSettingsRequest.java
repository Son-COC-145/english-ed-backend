package com.example.english_app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.Set;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSettingsRequest {

    private static final Set<Short> ALLOWED_XP_VALUES = Set.of(
            (short) 10, (short) 20, (short) 30, (short) 50
    );

    @NotNull(message = "Mục tiêu XP không được để trống")
    @Schema(description = "Mục tiêu XP hàng ngày. Chỉ chấp nhận: 10, 20, 30, 50", example = "20")
    private Short dailyGoalXp;

    @Schema(type = "string", example = "20:30:00", description = "Thời gian nhắc nhở học hàng ngày (định dạng HH:mm:ss)")
    private LocalTime reminderTime;

    @JsonIgnore
    public boolean isValidDailyGoalXp() {
        return dailyGoalXp != null && ALLOWED_XP_VALUES.contains(dailyGoalXp);
    }
}

