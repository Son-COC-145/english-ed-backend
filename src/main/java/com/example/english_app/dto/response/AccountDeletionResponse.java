package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.AccountDeletionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionResponse {
    private Long requestId;
    private AccountDeletionStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime completedAt;
}
