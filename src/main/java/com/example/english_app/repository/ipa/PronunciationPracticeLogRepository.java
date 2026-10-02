package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PronunciationPracticeLogRepository extends JpaRepository<PronunciationPracticeLog, Long> {

    @Query("SELECT w.phoneme.id AS phonemeId, AVG(l.overallScore) AS avgScore, MAX(l.overallScore) AS maxScore, COUNT(l.id) AS practiceCount " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId " +
           "GROUP BY w.phoneme.id")
    List<PhonemeStatProjection> findPhonemeStatsByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT l, w.word, w.ipaTranscription " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND w.phoneme.id = :phonemeId " +
           "ORDER BY l.practicedAt DESC")
    List<Object[]> findHistoryByStudentAndPhonemeRaw(@Param("studentId") Long studentId, @Param("phonemeId") Short phonemeId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT w.phoneme.id) " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId " +
           "AND w.phoneme.id IN :phonemeIds")
    long countPracticedPhonemeIds(
            @Param("studentId") Long studentId,
            @Param("phonemeIds") List<Short> phonemeIds);
}

