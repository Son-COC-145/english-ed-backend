package com.example.english_app.service.gamification;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.MinigameSubmitRequest;
import com.example.english_app.dto.response.MinigameSubmitResponse;
import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.enums.LearningStatus;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.MinigameResult;
import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.MinigameResultRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.english_app.dto.response.MinigameResultDetailResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.StudentVocabularyProgressResponse;

import java.util.List;
import java.util.ArrayList;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import com.example.english_app.dto.response.DailyMissionResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.classroom.SyllabusItemRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameficationService {
    private final MinigameResultRepository minigameResultRepository;
    private final StudentStatRepository studentStatRepository;
    private final StudentVocabularyProgressRepository studentVocabularyProgressRepository;
    private final VocabularyRepository vocabularyRepository;
    private final UserRepository userRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final SyllabusItemRepository syllabusItemRepository;

    @Value("${gamification.daily-mission.new-words-limit:5}")
    private int newWordsLimit;

    @Value("${gamification.daily-mission.review-words-limit:15}")
    private int reviewWordsLimit;

    @Transactional
    public MinigameSubmitResponse procesGameSubmit(MinigameSubmitRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        Vocabulary vocabulary = vocabularyRepository.findById(request.getVocabularyId())
                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());

        // Cập nhật thống kê người học & tính xp
        StudentStat stat = studentStatRepository.findByStudentId(user.getId())
                .orElse(StudentStat.builder().student(user).build());

        short xpEarned = calculateXp(request.getGameType(), request.getDurationSeconds(), request.getIsCorrect());

        stat.setTotalXp(stat.getTotalXp() + xpEarned);
        updateStreakLogic(stat);
        studentStatRepository.save(stat);

        // Xử lý tracking từ vựng
        StudentVocabularyProgress progress = studentVocabularyProgressRepository
                .findByStudentIdAndVocabularyId(user.getId(), vocabulary.getId())
                .orElse(StudentVocabularyProgress.builder()
                        .student(user)
                        .vocabulary(vocabulary)
                        .status(LearningStatus.NEW)
                        .build());

        updateVocabularyProgress(progress, request.getIsCorrect());
        studentVocabularyProgressRepository.save(progress);

        // Ghi log ket qua
        MinigameResult result = MinigameResult.builder()
                .student(user)
                .gameType(request.getGameType())
                .topic(vocabulary.getTopic())
                .score((short) (request.getIsCorrect() ? 100 : 0))
                .xpEarned(xpEarned)
                .durationSeconds(request.getDurationSeconds())
                .build();
        minigameResultRepository.save(result);

        return MinigameSubmitResponse.builder()
                .xpEarned(xpEarned)
                .totalXp(stat.getTotalXp())
                .currentStreak(stat.getCurrentStreak())
                .newVocabularyStatus(progress.getStatus())
                .resultId(result.getId()) // Trả về ID của kết quả
                .build();
    }

    private short calculateXp(GameType type, short duration, boolean isCorrect) {
        if (!isCorrect)
            return 0;

        int baseXp = switch (type) {
            case LISTEN_CHOOSE -> 10;
            case WORD_SCRAMBLE -> 15;
            case FILL_CONTEXT -> 20;
            case MATCHING_FLASH -> 10;
            default -> 5;
        };

        if (duration <= 3) {
            baseXp += 5;
        }

        return (short) baseXp;
    }

    // Logic cập nhật tiến độ học
    private void updateStreakLogic(StudentStat stat) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = stat.getLastActivityDate();
        if (lastDate == null) {
            stat.setCurrentStreak((short) 1);
        } else if (lastDate.equals(today.minusDays(1))) {
            // Học liên tục
            stat.setCurrentStreak((short) (stat.getCurrentStreak() + 1));
        } else {
            // Đã đứt chuỗi
            stat.setCurrentStreak((short) 1);
        }
        stat.setLastActivityDate(today);

        if (stat.getCurrentStreak() > stat.getLongestStreak()) {
            stat.setCurrentStreak(stat.getCurrentStreak());
        }
    }

    // Logic chuyển đổi trạng thái từ vựng
    private void updateVocabularyProgress(StudentVocabularyProgress progress, boolean isCorrect) {
        if (isCorrect) {
            progress.setCorrectCount((short) (progress.getCorrectCount() + 1));
            // Trả lời đúng 3 lần => Đánh dẫu là đã thuộc
            if (progress.getCorrectCount() >= 3) {
                progress.setStatus(LearningStatus.MASTERED);
            } else if (progress.getStatus() == LearningStatus.NEW) {
                progress.setStatus(LearningStatus.LEARNING);
            }
        } else {
            progress.setIncorrectCount((short) (progress.getIncorrectCount() + 1));
            progress.setStatus(LearningStatus.REVIEWING);
        }

        progress.setLastPracticedAt(LocalDateTime.now());

        // Thuật toán SM-2 để tính nextReviewAt
        int quality = isCorrect ? 4 : 2;

        float ef = progress.getEasinessFactor() != null ? progress.getEasinessFactor() : 2.5f;
        int rep = progress.getRepetitions() != null ? progress.getRepetitions() : 0;
        int interval = progress.getIntervalDays() != null ? progress.getIntervalDays() : 0;

        if (quality >= 3) {
            if (rep == 0) {
                interval = 1;
            } else if (rep == 1) {
                interval = 6;
            } else {
                interval = Math.round(interval * ef);
            }
            rep++;
        } else {
            rep = 0;
            interval = 1;
        }

        ef = (float) (ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
        if (ef < 1.3f) {
            ef = 1.3f;
        }

        progress.setEasinessFactor(ef);
        progress.setRepetitions(rep);
        progress.setIntervalDays(interval);

        progress.setNextReviewAt(LocalDateTime.now().plusDays(interval));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    public StudentStatResponse getStudentStat() {
        User user = getCurrentUser();
        StudentStat stat = studentStatRepository.findByStudentId(user.getId())
                .orElse(StudentStat.builder().student(user).build());
        return StudentStatResponse.builder()
                .totalXp(stat.getTotalXp())
                .currentStreak(stat.getCurrentStreak())
                .longestStreak(stat.getLongestStreak())
                .streakFreezeCount(stat.getStreakFreezeCount())
                .lastActivityDate(stat.getLastActivityDate())
                .totalStudyMinutes(stat.getTotalStudyMinutes())
                .updatedAt(stat.getUpdatedAt())
                .build();
    }

    public PageResponse<StudentVocabularyProgressResponse> getVocabularyProgresses(Pageable pageable) {
        User user = getCurrentUser();
        Page<StudentVocabularyProgress> page = studentVocabularyProgressRepository.findByStudentId(user.getId(),
                pageable);

        List<StudentVocabularyProgressResponse> responses = page.getContent().stream()
                .map(p -> StudentVocabularyProgressResponse.builder()
                        .id(p.getId())
                        .vocabularyId(p.getVocabulary().getId())
                        .word(p.getVocabulary().getWord())
                        .status(p.getStatus())
                        .nextReviewAt(p.getNextReviewAt())
                        .correctCount(p.getCorrectCount())
                        .incorrectCount(p.getIncorrectCount())
                        .lastPracticedAt(p.getLastPracticedAt())
                        .easinessFactor(p.getEasinessFactor())
                        .intervalDays(p.getIntervalDays())
                        .repetitions(p.getRepetitions())
                        .build())
                .toList();

        return PageResponse.<StudentVocabularyProgressResponse>builder()
                .content(responses)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public StudentVocabularyProgressResponse getVocabularyProgress(Long id) {
        User user = getCurrentUser();
        StudentVocabularyProgress p = studentVocabularyProgressRepository.findByIdAndStudentId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Vocabulary progress not found"));
        return StudentVocabularyProgressResponse.builder()
                .id(p.getId())
                .vocabularyId(p.getVocabulary().getId())
                .word(p.getVocabulary().getWord())
                .status(p.getStatus())
                .nextReviewAt(p.getNextReviewAt())
                .correctCount(p.getCorrectCount())
                .incorrectCount(p.getIncorrectCount())
                .lastPracticedAt(p.getLastPracticedAt())
                .easinessFactor(p.getEasinessFactor())
                .intervalDays(p.getIntervalDays())
                .repetitions(p.getRepetitions())
                .build();
    }

    public PageResponse<MinigameResultDetailResponse> getMinigameResults(Pageable pageable) {
        User user = getCurrentUser();
        Page<MinigameResult> page = minigameResultRepository.findByStudentId(user.getId(), pageable);

        List<MinigameResultDetailResponse> responses = page.getContent().stream()
                .map(r -> MinigameResultDetailResponse.builder()
                        .id(r.getId())
                        .gameType(r.getGameType())
                        .topicId(r.getTopic() != null ? r.getTopic().getId().longValue() : null)
                        .topicName(r.getTopic() != null ? r.getTopic().getNameEn() : null)
                        .score(r.getScore())
                        .xpEarned(r.getXpEarned())
                        .durationSeconds(r.getDurationSeconds())
                        .playedAt(r.getPlayedAt())
                        .build())
                .toList();

        return PageResponse.<MinigameResultDetailResponse>builder()
                .content(responses)
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    public MinigameResultDetailResponse getMinigameResult(Long id) {
        User user = getCurrentUser();
        MinigameResult r = minigameResultRepository.findByIdAndStudentId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Minigame result not found"));
        return MinigameResultDetailResponse.builder()
                .id(r.getId())
                .gameType(r.getGameType())
                .topicId(r.getTopic() != null ? r.getTopic().getId().longValue() : null)
                .topicName(r.getTopic() != null ? r.getTopic().getNameEn() : null)
                .score(r.getScore())
                .xpEarned(r.getXpEarned())
                .durationSeconds(r.getDurationSeconds())
                .playedAt(r.getPlayedAt())
                .build();
    }

    public DailyMissionResponse getDailyMission() {
        User user = getCurrentUser();
        Long studentId = user.getId();

        // 1. Lấy danh sách ÔN TẬP
        Pageable reviewPage = PageRequest.of(0, reviewWordsLimit);
        List<Vocabulary> reviewVocabs = studentVocabularyProgressRepository
               .findVocabulariesToReview(studentId, LocalDateTime.now(), reviewPage).getContent();

        // 2. Tính toán Tuần học hiện tại để tìm TỪ MỚI
        List<Course> activeCourses = courseStudentRepository.findActiveCoursesByStudentId(studentId);
        List<Vocabulary> newVocabs = new ArrayList<>();
        
        if (!activeCourses.isEmpty()) {
            List<Long> courseIds = new ArrayList<>();
            List<Short> weekNumbers = new ArrayList<>();
            
            for (Course c : activeCourses) {
                if (c.getStartDate() != null) {
                    long daysBetween = ChronoUnit.DAYS.between(c.getStartDate(), LocalDate.now());
                    short weekNum = (short) ((Math.max(0, daysBetween) / 7) + 1);
                    
                    courseIds.add(c.getId());
                    weekNumbers.add(weekNum);
                }
            }

            if (!courseIds.isEmpty()) {
                List<Long> topicIds = syllabusItemRepository.findTopicIdsByCoursesAndWeeks(courseIds, weekNumbers);

                if (!topicIds.isEmpty()) {
                    newVocabs = vocabularyRepository.findRandomNewVocabularies(topicIds, studentId, newWordsLimit);
                }
            }
        }

        return DailyMissionResponse.builder()
                .reviewWords(reviewVocabs.stream().map(this::mapToVocabularyResponse).toList())
                .newWords(newVocabs.stream().map(this::mapToVocabularyResponse).toList())
                .build();
    }

    private VocabularyResponse mapToVocabularyResponse(Vocabulary v) {
        VocabularyResponse.TopicBrief topicBrief = null;
        if (v.getTopic() != null) {
            topicBrief = VocabularyResponse.TopicBrief.builder()
                    .id(v.getTopic().getId())
                    .nameEn(v.getTopic().getNameEn())
                    .nameVi(v.getTopic().getNameVi())
                    .build();
        }
        VocabularyResponse.UserBrief userBrief = null;
        if (v.getCreatedBy() != null) {
            userBrief = VocabularyResponse.UserBrief.builder()
                    .id(v.getCreatedBy().getId())
                    .fullName(v.getCreatedBy().getFullName())
                    .email(v.getCreatedBy().getEmail())
                    .build();
        }
        return VocabularyResponse.builder()
                .id(v.getId())
                .topic(topicBrief)
                .word(v.getWord())
                .ipaTranscription(v.getIpaTranscription())
                .cefrLevel(v.getCefrLevel() != null ? v.getCefrLevel().name() : null)
                .definitionVi(v.getDefinitionVi())
                .imageUrl(v.getImageUrl())
                .audioUsUrl(v.getAudioUsUrl())
                .audioUkUrl(v.getAudioUkUrl())
                .collocationJson(v.getCollocationJson())
                .nuanceNote(v.getNuanceNote())
                .exampleSentencesJson(v.getExampleSentencesJson())
                .dialogueJson(v.getDialogueJson())
                .status(v.getStatus() != null ? v.getStatus().name() : null)
                .createdBy(userBrief)
                .publishedAt(v.getPublishedAt())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
