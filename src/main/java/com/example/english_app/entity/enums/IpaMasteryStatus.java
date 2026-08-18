package com.example.english_app.entity.enums;

public enum IpaMasteryStatus {
    MASTERED,       // >= 80% (Green)
    LEARNING,       // >= 60% && < 80% (Yellow)
    NEEDS_PRACTICE, // < 60% (Red)
    UNLEARNED       // Chưa luyện tập lần nào (Gray)
}
