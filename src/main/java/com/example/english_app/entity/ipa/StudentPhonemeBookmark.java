package com.example.english_app.entity.ipa;

import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_phoneme_bookmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPhonemeBookmark {

    @EmbeddedId
    private StudentPhonemeBookmarkId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("studentId")
    @JoinColumn(name = "student_id")
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("phonemeId")
    @JoinColumn(name = "phoneme_id")
    private IpaPhoneme phoneme;

    @CreationTimestamp
    @Column(name = "bookmarked_at", nullable = false, updatable = false)
    private LocalDateTime bookmarkedAt;
}
