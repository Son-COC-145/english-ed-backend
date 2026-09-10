package com.example.english_app.service.gamification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.english_app.dto.request.MinigameSubmitRequest;
import com.example.english_app.dto.request.ReviewSubmitRequest;
import com.example.english_app.dto.response.DailyMissionResponse;
import com.example.english_app.dto.response.DueReviewPageResponse;
import com.example.english_app.dto.response.MinigameResultDetailResponse;
import com.example.english_app.dto.response.MinigameSubmitResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.ReviewSubmitResponse;
import com.example.english_app.dto.response.StudentStatResponse;
import com.example.english_app.dto.response.StudentVocabularyProgressResponse;
import com.example.english_app.dto.response.VocabularyDueReviewResponse;
import com.example.english_app.dto.response.VocabularyResponse;
import com.example.english_app.dto.response.VocabularySummaryResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.enums.LearningStatus;
import com.example.english_app.entity.enums.ReviewRating;
import com.example.english_app.entity.gamification.IdempotencyKey;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.user.User;
import com.example.english_app.entity.vocabulary.MinigameResult;
import com.example.english_app.entity.vocabulary.StudentVocabularyProgress;
import com.example.english_app.entity.vocabulary.Vocabulary;
import com.example.english_app.entity.classroom.Course;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.IdempotencyKeyRepository;
import com.example.english_app.repository.gamification.MinigameResultRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.vocabulary.VocabularyRepository;
import com.example.english_app.repository.classroom.CourseStudentRepository;
import com.example.english_app.repository.classroom.SyllabusItemRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${gamification.daily-mission.new-words-limit:5}")
    private int newWordsLimit;

    @Value("${gamification.daily-mission.review-words-limit:15}")
    private int reviewWordsLimit;

    // ═══════════════════════════════════════════════════════════════════════
    // Mini-game Submit (backward compatible + idempotency)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public MinigameSubmitResponse procesGameSubmit(MinigameSubmitRequest request) {
        User user = getCurrentUser();

        // Idempotency check
        if (request.getAttemptId() != null) {
            var cached = idempotencyKeyRepository
                    .findByUserIdAndAttemptId(user.getId(), request.getAttemptId());
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get().getResultJson(), MinigameSubmitResponse.class);
                } catch (JsonProcessingException e) {
                    // Nếu deserialize lỗi thì xử lý bình thường (edge case)
                }
            }
        }

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

        updateVocabularyProgressFromMinigame(progress, request.getIsCorrect());
        studentVocabularyProgressRepository.save(progress);

        // Ghi log kết quả
        MinigameResult result = MinigameResult.builder()
                .student(user)
                .gameType(request.getGameType())
                .topic(vocabulary.getTopic())
                .score((short) (request.getIsCorrect() ? 100 : 0))
                .xpEarned(xpEarned)
                .durationSeconds(request.getDurationSeconds())
                .build();
        minigameResultRepository.save(result);

        MinigameSubmitResponse response = MinigameSubmitResponse.builder()
                .xpEarned(xpEarned)
                .totalXp(stat.getTotalXp())
                .currentStreak(stat.getCurrentStreak())
                .newVocabularyStatus(progress.getStatus())
                .resultId(result.getId())
                .build();

        // Cache idempotency result
        if (request.getAttemptId() != null) {
            cacheIdempotencyResult(user.getId(), request.getAttemptId(), response);
        }

        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SRS Review Submit (AGAIN / HARD / GOOD / EASY)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public ReviewSubmitResponse processReviewSubmit(ReviewSubmitRequest request) {
        User user = getCurrentUser();

        // Idempotency check
        if (request.getAttemptId() != null) {
            var cached = idempotencyKeyRepository
                    .findByUserIdAndAttemptId(user.getId(), request.getAttemptId());
            if (cached.isPresent()) {
                try {
                    return objectMapper.readValue(cached.get().getResultJson(), ReviewSubmitResponse.class);
                } catch (JsonProcessingException e) {
                    // fallthrough – xử lý bình thường
                }
            }
        }

        // Load vocabulary
        Vocabulary vocab = vocabularyRepository.findById(request.getVocabularyId())
                .orElseThrow(() -> ErrorCode.VOCABULARY_NOT_FOUND.toException());

        // Load hoặc tạo progress
        StudentVocabularyProgress progress = studentVocabularyProgressRepository
                .findByStudentIdAndVocabularyId(user.getId(), vocab.getId())
                .orElseThrow(() -> ErrorCode.VOCABULARY_PROGRESS_NOT_FOUND.toException());

        LearningStatus previousStatus = progress.getStatus();

        // Áp dụng SM-2 với 4-mức rating
        applySmTwoAlgorithm(progress, request.getRating());
        studentVocabularyProgressRepository.save(progress);

        // XP theo mức: AGAIN=0, HARD=3, GOOD=5, EASY=8
        short xpEarned = switch (request.getRating()) {
            case AGAIN -> 0;
            case HARD  -> 2;
            case FAIR  -> 4;
            case GOOD  -> 5;
            case EASY  -> 8;
        };

        StudentStat stat = studentStatRepository.findByStudentId(user.getId())
                .orElse(StudentStat.builder().student(user).build());
        stat.setTotalXp(stat.getTotalXp() + xpEarned);
        updateStreakLogic(stat);
        studentStatRepository.save(stat);

        ReviewSubmitResponse response = ReviewSubmitResponse.builder()
                .vocabularyId(vocab.getId())
                .previousStatus(previousStatus)
                .newStatus(progress.getStatus())
                .nextReviewAt(progress.getNextReviewAt())
                .intervalDays(progress.getIntervalDays())
                .xpEarned(xpEarned)
                .totalXp(stat.getTotalXp())
                .currentStreak(stat.getCurrentStreak())
                .build();

        // Cache idempotency result
        if (request.getAttemptId() != null) {
            cacheIdempotencyResult(user.getId(), request.getAttemptId(), response);
        }

        return response;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SRS Due-Review Queue
    // ═══════════════════════════════════════════════════════════════════════

    public DueReviewPageResponse getDueReviews(int page, int size, Short topicId, CefrLevel cefrLevel) {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size);

        long dueCount = studentVocabularyProgressRepository
                .countDueReviews(user.getId(), now);

        Page<StudentVocabularyProgress> dueItems = studentVocabularyProgressRepository
                .findDueReviews(user.getId(), now, topicId, cefrLevel, pageable);

        List<VocabularyDueReviewResponse> items = dueItems.getContent().stream()
                .map(p -> {
                    Vocabulary v = p.getVocabulary();
                    return VocabularyDueReviewResponse.builder()
                            .progressId(p.getId())
                            .status(p.getStatus())
                            .nextReviewAt(p.getNextReviewAt())
                            .vocabularyId(v.getId())
                            .word(v.getWord())
                            .ipaTranscription(v.getIpaTranscription())
                            .definitionVi(v.getDefinitionVi())
                            .imageUrl(v.getImageUrl())
                            .audioUsUrl(v.getAudioUsUrl())
                            .audioUkUrl(v.getAudioUkUrl())
                            .cefrLevel(v.getCefrLevel() != null ? v.getCefrLevel().name() : null)
                            .topicId(v.getTopic() != null ? v.getTopic().getId().longValue() : null)
                            .topicNameEn(v.getTopic() != null ? v.getTopic().getNameEn() : null)
                            .build();
                })
                .toList();

        return DueReviewPageResponse.builder()
                .dueCount(dueCount)
                .items(items)
                .page(dueItems.getNumber())
                .size(dueItems.getSize())
                .totalElements(dueItems.getTotalElements())
                .totalPages(dueItems.getTotalPages())
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Vocabulary Summary (cho Home screen)
    // ═══════════════════════════════════════════════════════════════════════

    public VocabularySummaryResponse getVocabularySummary() {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        Long studentId = user.getId();

        // Đếm theo status trong 1 query
        List<Object[]> statusCounts = studentVocabularyProgressRepository
                .countByStudentIdGroupByStatus(studentId);

        long newCount = 0, learningCount = 0, reviewingCount = 0, masteredCount = 0;
        for (Object[] row : statusCounts) {
            LearningStatus status = (LearningStatus) row[0];
            long count = (long) row[1];
            switch (status) {
                case NEW      -> newCount = count;
                case LEARNING -> learningCount = count;
                case REVIEWING-> reviewingCount = count;
                case MASTERED -> masteredCount = count;
            }
        }

        long total = newCount + learningCount + reviewingCount + masteredCount;
        long dueTodayCount = studentVocabularyProgressRepository.countDueReviews(studentId, now);

        return VocabularySummaryResponse.builder()
                .total(total)
                .newCount(newCount)
                .learningCount(learningCount)
                .reviewingCount(reviewingCount)
                .masteredCount(masteredCount)
                .dueTodayCount(dueTodayCount)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Vocabulary Progress (với filter)
    // ═══════════════════════════════════════════════════════════════════════

    public PageResponse<StudentVocabularyProgressResponse> getVocabularyProgresses(
            LearningStatus status, boolean dueOnly, Pageable pageable) {
        User user = getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        Page<StudentVocabularyProgress> page = studentVocabularyProgressRepository
                .findByStudentIdWithFilters(user.getId(), status, dueOnly, now, pageable);

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
        StudentVocabularyProgress p = studentVocabularyProgressRepository
                .findByIdAndStudentId(id, user.getId())
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
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Minigame Results
    // ═══════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // Gamification Stats
    // ═══════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // Daily Mission
    // ═══════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═══════════════════════════════════════════════════════════════════════

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());
    }

    private short calculateXp(GameType type, short duration, boolean isCorrect) {
        if (!isCorrect) return 0;

        int baseXp = switch (type) {
            case LISTEN_CHOOSE  -> 10;
            case WORD_SCRAMBLE  -> 15;
            case FILL_CONTEXT   -> 20;
            case MATCHING_FLASH -> 10;
            default             -> 5;
        };

        if (duration <= 3) baseXp += 5;
        return (short) baseXp;
    }

    /** Cập nhật streak và longestStreak (bug fix: setLongestStreak thay vì setCurrentStreak) */
    private void updateStreakLogic(StudentStat stat) {
        LocalDate today = LocalDate.now();
        LocalDate lastDate = stat.getLastActivityDate();

        if (lastDate == null) {
            stat.setCurrentStreak((short) 1);
        } else if (lastDate.equals(today.minusDays(1))) {
            stat.setCurrentStreak((short) (stat.getCurrentStreak() + 1));
        } else if (!lastDate.equals(today)) {
            // Đứt chuỗi (không reset nếu đã học hôm nay)
            stat.setCurrentStreak((short) 1);
        }
        stat.setLastActivityDate(today);

        if (stat.getCurrentStreak() > stat.getLongestStreak()) {
            stat.setLongestStreak(stat.getCurrentStreak()); // BUG FIX
        }
    }

    /**
     * SM-2 với mini-game (boolean correct/incorrect) – giữ backward compatible.
     * quality: đúng=4, sai=2 (đơn giản hóa so với 0-5 scale)
     */
    private void updateVocabularyProgressFromMinigame(StudentVocabularyProgress progress, boolean isCorrect) {
        if (isCorrect) {
            progress.setCorrectCount((short) (progress.getCorrectCount() + 1));
            if (progress.getCorrectCount() >= 3) {
                progress.setStatus(LearningStatus.MASTERED);
            } else if (progress.getStatus() == LearningStatus.NEW) {
                progress.setStatus(LearningStatus.LEARNING);
            }
        } else {
            progress.setIncorrectCount((short) (progress.getIncorrectCount() + 1));
            if (progress.getStatus() != LearningStatus.NEW) {
                progress.setStatus(LearningStatus.REVIEWING);
            }
        }
        progress.setLastPracticedAt(LocalDateTime.now());

        // SM-2 với quality map đơn giản (mini-game chỉ có đúng/sai)
        applySmTwoAlgorithm(progress, isCorrect ? ReviewRating.GOOD : ReviewRating.AGAIN);
    }

    /**
     * SM-2 chuẩn với 5-mức rating từ Flashcard review.
     * Đây là source of truth cho lịch ôn tập.
     */
    private void applySmTwoAlgorithm(StudentVocabularyProgress progress, ReviewRating rating) {
        int quality = switch (rating) {
            case AGAIN -> 0;
            case HARD  -> 1;
            case FAIR  -> 3;
            case GOOD  -> 4;
            case EASY  -> 5;
        };

        float ef = progress.getEasinessFactor() != null ? progress.getEasinessFactor() : 2.5f;
        int rep = progress.getRepetitions() != null ? progress.getRepetitions() : 0;
        int interval = progress.getIntervalDays() != null ? progress.getIntervalDays() : 0;

        if (quality >= 3) {
            interval = switch (rep) {
                case 0  -> 1;
                case 1  -> 6;
                default -> Math.round(interval * ef);
            };
            rep++;
        } else {
            // AGAIN hoặc HARD dưới 3 – reset về đầu
            rep = 0;
            interval = 1;
        }

        ef = (float) (ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));
        ef = Math.max(1.3f, ef);

        progress.setEasinessFactor(ef);
        progress.setRepetitions(rep);
        progress.setIntervalDays(interval);
        progress.setNextReviewAt(LocalDateTime.now().plusDays(interval));
        progress.setLastPracticedAt(LocalDateTime.now());

        // Cập nhật status theo kết quả review
        if (quality == 0) {
            // AGAIN → từ này cần học lại
            progress.setStatus(LearningStatus.LEARNING);
        } else if (rep >= 3 && quality >= 4) {
            progress.setStatus(LearningStatus.MASTERED);
        } else if (rep >= 1) {
            progress.setStatus(LearningStatus.REVIEWING);
        }
    }

    /** Cache kết quả vào idempotency_keys để retry an toàn */
    private void cacheIdempotencyResult(Long userId, String attemptId, Object responseObj) {
        try {
            String resultJson = objectMapper.writeValueAsString(responseObj);
            IdempotencyKey key = IdempotencyKey.builder()
                    .attemptId(attemptId)
                    .userId(userId)
                    .resultJson(resultJson)
                    .createdAt(LocalDateTime.now())
                    .build();
            idempotencyKeyRepository.save(key);
        } catch (JsonProcessingException e) {
            // Non-critical: nếu cache lỗi thì không fail request chính
        }
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
