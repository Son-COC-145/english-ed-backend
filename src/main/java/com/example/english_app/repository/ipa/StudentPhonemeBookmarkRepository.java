package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.StudentPhonemeBookmark;
import com.example.english_app.entity.ipa.StudentPhonemeBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPhonemeBookmarkRepository extends JpaRepository<StudentPhonemeBookmark, StudentPhonemeBookmarkId> {
}
