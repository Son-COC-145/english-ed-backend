package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pure-function factory: tạo PlacementResultResponse từ dữ liệu thô.
 *
 * <p><b>Không có @Transactional</b> — class này không đụng DB.
 * Không có dependency vào bất kỳ Service nào khác → zero circular dependency risk.
 *
 * <p>Tách ra khỏi OnboardingService/PlacementTestService để loại bỏ code lặp
 * giữa completePlacementTest() và getPlacementResult() (Task M3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlacementResultFactory {

    private final ObjectMapper objectMapper;

    // ─── Score Calculation ────────────────────────────────────────────────────

    /**
     * Tính điểm % cho một kỹ năng từ danh sách câu trả lời.
     *
     * @param answers Danh sách câu trả lời của kỹ năng đó (có thể null/empty).
     * @return Điểm 0–100.
     */
    public short calculateSkillScore(List<PlacementTestAnswer> answers) {
        if (answers == null || answers.isEmpty()) return 0;
        long correct = answers.stream().filter(PlacementTestAnswer::getIsCorrect).count();
        return (short) Math.round((double) correct / answers.size() * 100);
    }

    /**
     * Tính điểm tất cả kỹ năng từ danh sách câu trả lời gộp.
     * Group by Skill rồi delegate sang calculateSkillScore().
     */
    public SkillScores calculateAllSkills(List<PlacementTestAnswer> answers) {
        Map<Skill, List<PlacementTestAnswer>> bySkill = answers.stream()
                // Lọc các answer bị orphan (question null) hoặc question thiếu skill field
                // để tránh NullPointerException khi CAT early-stop gọi completeTest()
                .filter(a -> a.getQuestion() != null && a.getQuestion().getSkill() != null)
                .collect(Collectors.groupingBy(a -> a.getQuestion().getSkill()));

        return SkillScores.builder()
                .vocab(calculateSkillScore(bySkill.get(Skill.VOCABULARY)))
                .grammar(calculateSkillScore(bySkill.get(Skill.GRAMMAR)))
                .reading(calculateSkillScore(bySkill.get(Skill.READING)))
                .listening(calculateSkillScore(bySkill.get(Skill.LISTENING)))
                .pronunciation(calculateSkillScore(bySkill.get(Skill.PRONUNCIATION)))
                .build();
    }

    // ─── Response Builder ─────────────────────────────────────────────────────

    /**
     * Xây dựng PlacementResultResponse từ dữ liệu đã tính sẵn.
     * Dùng chung cho cả completePlacementTest() và getPlacementResult() — DRY.
     */
    public PlacementResultResponse buildResponse(
            CefrLevel cefrLevel,
            SkillScores scores,
            List<PlacementTestAnswer> answers,
            String roadmapJson,
            boolean roadmapGenerated) {

        Map<String, Short> radarData = buildSkillScoreMap(scores);
        List<String> suggestedModules = extractSuggestedModules(roadmapJson, cefrLevel);
        List<String> diagnosticTips = buildDiagnosticTips(scores, cefrLevel);

        int totalCorrect = (int) answers.stream().filter(PlacementTestAnswer::getIsCorrect).count();

        return PlacementResultResponse.builder()
                .cefrLevel(cefrLevel.name())
                .totalQuestions(answers.size())
                .correctAnswers(totalCorrect)
                .vocabScore(scores.getVocab())
                .grammarScore(scores.getGrammar())
                .readingScore(scores.getReading())
                .listeningScore(scores.getListening())
                .pronunciationScore(scores.getPronunciation())
                .radarChartData(radarData)
                .message(buildResultMessage(cefrLevel))
                .cefrDescription(buildCefrDescription(cefrLevel))
                .strengths(getTopSkills(radarData, true))
                .weaknesses(getTopSkills(radarData, false))
                .roadmapGenerated(roadmapGenerated)
                .suggestedModules(suggestedModules)
                .diagnosticTips(diagnosticTips)
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Map<String, Short> buildSkillScoreMap(SkillScores s) {
        Map<String, Short> map = new LinkedHashMap<>();
        map.put("Từ vựng",  s.getVocab());
        map.put("Ngữ pháp", s.getGrammar());
        map.put("Đọc hiểu", s.getReading());
        map.put("Nghe",     s.getListening());
        map.put("Phát âm",  s.getPronunciation());
        return map;
    }

    private List<String> getTopSkills(Map<String, Short> scores, boolean getStrengths) {
        List<Map.Entry<String, Short>> sorted = new ArrayList<>(scores.entrySet());
        if (getStrengths) {
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        } else {
            sorted.sort(Map.Entry.comparingByValue());
        }
        return sorted.stream()
                .filter(e -> !getStrengths || e.getValue() > 0)
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> extractSuggestedModules(String roadmapJson, CefrLevel fallbackLevel) {
        if (roadmapJson != null) {
            try {
                RoadmapResponse roadmap = objectMapper.readValue(roadmapJson, RoadmapResponse.class);
                if (roadmap.getMilestones() != null && !roadmap.getMilestones().isEmpty()) {
                    return roadmap.getMilestones().get(0).getModules().stream()
                            .map(RoadmapModule::getTitle)
                            .collect(Collectors.toList());
                }
            } catch (Exception e) {
                log.warn("Failed to parse roadmap_json, falling back to default modules. Error: {}", e.getMessage());
            }
        }
        return buildSuggestedModules(fallbackLevel);
    }

    private List<String> buildSuggestedModules(CefrLevel level) {
        return switch (level) {
            case A1, A2 -> List.of("Từ vựng cơ bản", "Phát âm IPA nền", "Speaking cơ bản");
            case B1     -> List.of("Từ vựng chủ đề", "Speaking tình huống", "Phát âm âm đuôi");
            case B2     -> List.of("Speaking nâng cao", "Từ vựng học thuật", "Phát âm ngữ điệu");
            case C1     -> List.of("Speaking đàm phán / thuyết trình", "Luyện thi chứng chỉ");
            case C2     -> List.of("Speaking học thuật chuyên sâu", "Luyện thi chứng chỉ nâng cao");
        };
    }

    private String buildResultMessage(CefrLevel level) {
        return switch (level) {
            case A1 -> "Bạn đang ở trình độ Sơ cấp (A1). Hãy bắt đầu với những từ vựng và cấu trúc cơ bản nhất!";
            case A2 -> "Bạn đang ở trình độ Sơ cấp (A2). Bạn đã nắm được căn bản, hãy củng cố thêm!";
            case B1 -> "Bạn đang ở trình độ Trung cấp (B1). Bạn có thể giao tiếp trong các tình huống quen thuộc!";
            case B2 -> "Bạn đang ở trình độ Trung cấp cao (B2). Hãy thử thách với các chủ đề phức tạp hơn!";
            case C1 -> "Xuất sắc! Bạn đang ở trình độ Cao cấp (C1). Hãy hoàn thiện kỹ năng nâng cao!";
            case C2 -> "Tuyệt vời! Bạn đang ở trình độ Thành thạo (C2). Hãy duy trì và phát triển năng lực học thuật chuyên sâu!";
        };
    }

    private String buildCefrDescription(CefrLevel level) {
        return switch (level) {
            case A1, A2 -> "Mới bắt đầu, vốn từ < 500 từ";
            case B1     -> "Giao tiếp đơn giản, hiểu ngữ cảnh quen thuộc";
            case B2     -> "Tự tin giao tiếp hầu hết tình huống thường gặp";
            case C1     -> "Thành thạo, xử lý được nội dung phức tạp";
            case C2     -> "Thông thạo gần như người bản ngữ, xử lý linh hoạt nội dung chuyên sâu";
        };
    }

    private List<String> buildDiagnosticTips(SkillScores scores, CefrLevel level) {
        List<String> tips = new ArrayList<>();
        if (scores.getPronunciation() < 60) {
            tips.add("Phát âm: Cần luyện kỹ bảng âm IPA (đặc biệt các cặp âm dễ nhầm như /iː/-/ɪ/, /s/-/ʃ/) để cải thiện độ chuẩn xác.");
        }
        if (scores.getListening() < 60) {
            tips.add("Kỹ năng nghe: Luyện nghe các đoạn hội thoại ngắn có phụ đề, chú ý nhận diện âm đuôi và nối âm.");
        }
        if (scores.getVocab() < 60) {
            tips.add("Từ vựng: Cần bổ sung 10-15 từ vựng chủ đề mỗi ngày và duy trì ôn tập lặp lại qua Flashcard.");
        }
        if (scores.getGrammar() < 60) {
            tips.add("Ngữ pháp: Củng cố lại các thì cơ bản (Hiện tại đơn, Quá khứ đơn) và cấu trúc câu thông dụng.");
        }
        if (scores.getReading() < 60) {
            tips.add("Đọc hiểu: Tập thói quen đọc các bài đọc ngắn và rèn luyện kỹ năng Skimming/Scanning để nắm ý chính.");
        }
        if (tips.isEmpty()) {
            tips.add("Nền tảng của bạn rất vững chắc! Hãy duy trì luyện phản xạ giao tiếp nâng cao và mở rộng vốn từ học thuật.");
        }
        return tips;
    }

    // ─── Value Object ─────────────────────────────────────────────────────────

    /**
     * Value object chứa điểm 5 kỹ năng. Tránh truyền 5 tham số rời rạc giữa các method.
     */
    @lombok.Value
    @lombok.Builder
    public static class SkillScores {
        short vocab;
        short grammar;
        short reading;
        short listening;
        short pronunciation;
    }
}
