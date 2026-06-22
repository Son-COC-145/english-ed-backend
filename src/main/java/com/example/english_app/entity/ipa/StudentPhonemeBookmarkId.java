package com.example.english_app.entity.ipa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPhonemeBookmarkId implements Serializable {

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "phoneme_id")
    private Short phonemeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentPhonemeBookmarkId that = (StudentPhonemeBookmarkId) o;
        return Objects.equals(studentId, that.studentId) &&
               Objects.equals(phonemeId, that.phonemeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, phonemeId);
    }
}
