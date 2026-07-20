package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanResponse {
    private Long id;
    private PlanName name;
    private BigDecimal price;
    private Integer durationDays;
    private Integer aiPromptLimit;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SubscriptionPlanResponse fromEntity(SubscriptionPlan entity) {
        if (entity == null) return null;
        return SubscriptionPlanResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .durationDays(entity.getDurationDays())
                .aiPromptLimit(entity.getAiPromptLimit())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
