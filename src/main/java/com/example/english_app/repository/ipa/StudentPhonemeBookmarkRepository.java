package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.StudentPhonemeBookmark;
import com.example.english_app.entity.ipa.StudentPhonemeBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentPhonemeBookmarkRepository extends JpaRepository<StudentPhonemeBookmark, StudentPhonemeBookmarkId> {

    /**
     * Lấy tất cả bookmark của học viên, JOIN FETCH phoneme để tránh N+1.
     * Sắp xếp theo thời điểm bookmark mới nhất trước.
     */
    @Query("SELECT b FROM StudentPhonemeBookmark b " +
           "JOIN FETCH b.phoneme " +
           "WHERE b.student.id = :studentId " +
           "ORDER BY b.bookmarkedAt DESC")
    List<StudentPhonemeBookmark> findByStudentId(@Param("studentId") Long studentId);
}

