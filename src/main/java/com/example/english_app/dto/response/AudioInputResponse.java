package com.example.english_app.dto.response;

public record AudioInputResponse(
        Long turnId,
        String status,
        String transcript
) {}
