package com.example.english_app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StudentStatResponse {
    private Integer totalXp;
    private Short currentStreak;
    private Short longestStreak;
    private Short streakFreezeCount;
    private LocalDate lastActivityDate;
    private Integer totalStudyMinutes;
    private LocalDateTime updatedAt;
}
