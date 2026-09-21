package com.example.english_app.entity.enums;

/**
 * Mức độ nhớ từ vựng mà học viên tự đánh giá sau khi xem Flashcard.
 * Backend map sang quality score của SM-2 (thang 0–5):
 *
 *  AGAIN -> 0  (hoàn toàn không nhớ)
 *  HARD  -> 1  (sai, nhưng nhìn đáp án thì quen)
 *  FAIR  -> 3  (đúng nhưng mất thời gian đáng kể)
 *  GOOD  -> 4  (đúng sau một chút do dự)
 *  EASY  -> 5  (đúng ngay, không cần suy nghĩ)
 */
public enum ReviewRating {
    /** Hoàn toàn không nhớ – lặp lại ngay hôm nay (quality = 0) */
    AGAIN,
    /** Sai, nhưng nhìn đáp án thì có vẻ quen (quality = 1) */
    HARD,
    /** Đúng nhưng mất thời gian đáng kể để nhớ ra (quality = 3) */
    FAIR,
    /** Đúng sau một chút do dự – interval tăng bình thường (quality = 4) */
    GOOD,
    /** Đúng ngay lập tức, không cần suy nghĩ (quality = 5) */
    EASY
}

