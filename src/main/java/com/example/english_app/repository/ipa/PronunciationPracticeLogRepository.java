package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface PronunciationPracticeLogRepository extends JpaRepository<PronunciationPracticeLog, Long> {

    long countByStudentIdAndPracticedAtGreaterThanEqual(Long studentId, LocalDateTime since);

    @Query("SELECT w.phoneme.id AS phonemeId, AVG(l.overallScore) AS avgScore, MAX(l.overallScore) AS maxScore, COUNT(l.id) AS practiceCount " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND l.practiceType = 'IPA_PHONEME' " +
           "GROUP BY w.phoneme.id")
    List<PhonemeStatProjection> findPhonemeStatsByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT l, w.word, w.ipaTranscription " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND w.phoneme.id = :phonemeId AND l.practiceType = 'IPA_PHONEME' " +
           "ORDER BY l.practicedAt DESC")
    List<Object[]> findHistoryByStudentAndPhonemeRaw(@Param("studentId") Long studentId, @Param("phonemeId") Short phonemeId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT w.phoneme.id) " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND l.practiceType = 'IPA_PHONEME' " +
           "AND w.phoneme.id IN :phonemeIds")
    long countPracticedPhonemeIds(
            @Param("studentId") Long studentId,
            @Param("phonemeIds") List<Short> phonemeIds);

    @Query("SELECT DISTINCT w.phoneme.id " +
           "FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND l.practiceType = 'IPA_PHONEME' " +
           "AND w.phoneme.id IN :phonemeIds")
    List<Short> findPracticedPhonemeIds(
            @Param("studentId") Long studentId,
            @Param("phonemeIds") List<Short> phonemeIds);

    @Query("SELECT COUNT(l.id) FROM PronunciationPracticeLog l " +
           "JOIN IpaExampleWord w ON l.refId = w.id " +
           "WHERE l.student.id = :studentId AND l.practiceType = 'IPA_PHONEME' " +
           "AND w.phoneme.id = :phonemeId AND l.practicedAt >= :since")
    long countPhonemePracticesSince(
            @Param("studentId") Long studentId,
            @Param("phonemeId") Short phonemeId,
            @Param("since") LocalDateTime since);
}

