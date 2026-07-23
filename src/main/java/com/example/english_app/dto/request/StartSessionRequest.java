package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartSessionRequest {
    @NotNull(message = "Vui lòng chọn kịch bản giao tiếp")
    private Short scenarioId;
}
