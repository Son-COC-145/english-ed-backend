package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.PlanName;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubscriptionPlanRequest {
    @NotNull(message = "Plan name is required")
    private PlanName name;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be >= 0")
    private BigDecimal price;

    @NotNull(message = "Duration days is required")
    @Min(value = 1, message = "Duration must be at least 1 day")
    private Integer durationDays;

    private Integer aiPromptLimit;

    private String description;
}
