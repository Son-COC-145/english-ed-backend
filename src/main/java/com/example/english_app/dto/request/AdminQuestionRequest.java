package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload tạo / chỉnh sửa câu hỏi Placement Test")
public class AdminQuestionRequest {

    @NotNull(message = "Trình độ CEFR không được để trống")
    @Schema(description = "Trình độ CEFR của câu hỏi", example = "A1")
    private CefrLevel cefrLevel;

    @NotNull(message = "Kỹ năng không được để trống")
    @Schema(description = "Kỹ năng kiểm tra", example = "GRAMMAR")
    private Skill skill;

    @NotNull(message = "Loại câu hỏi không được để trống")
    @Schema(description = "Loại câu hỏi. Xác định schema của contentJson (xem description bên dưới).",
            example = "MULTIPLE_CHOICE")
    private QuestionType questionType;

    @NotBlank(message = "Nội dung câu hỏi (JSON) không được để trống")
    @Schema(description = """
            Nội dung câu hỏi dạng JSON. Schema thay đổi theo questionType:

            **MULTIPLE_CHOICE / FILL_BLANK:**
            ```json
            {
              "question": "What is the past tense of 'go'?",
              "options": ["went", "goed", "gone", "going"],
              "explanation": "Irregular verb: go → went"
            }
            ```

            **READING_COMPREHENSION:**
            ```json
            {
              "passage": "Tom went to school every day...",
              "question": "Where did Tom go?",
              "options": ["school", "park", "store", "home"],
              "explanation": "The passage states Tom went to school."
            }
            ```

            **LISTENING:**
            ```json
            {
              "transcript": "Internal source text used only to pre-generate audio.",
              "audioUrl": "https://storage.../optional-pre-generated.mp3",
              "question": "What did the speaker say about the weather?",
              "options": ["It is sunny", "It is rainy", "It is cold", "It is hot"],
              "explanation": "The speaker mentioned it was raining."
            }
            ```

            **PRONUNCIATION:**
            ```json
            {
              "word": "comfortable",
              "ipaTranscription": "/ˈkʌmf.tə.bəl/",
              "instruction": "Đọc to từ bên dưới",
              "audioGuideUrl": "https://storage.../comfortable_guide.mp3"
            }
            ```
            """,
            example = "{\"question\":\"What is the past tense of go?\",\"options\":[\"went\",\"goed\",\"gone\",\"going\"]}")
    private String contentJson;

    @NotBlank(message = "Đáp án đúng không được để trống")
    @Schema(description = """
            Đáp án đúng của câu hỏi.
            - MULTIPLE_CHOICE / READING / LISTENING: một trong các giá trị trong mảng options. VD: "went"
            - FILL_BLANK: chuỗi đáp án. VD: "went"
            - PRONUNCIATION: từ tham chiếu mà người học cần đọc. VD: "comfortable"
            """,
            example = "went")
    private String correctAnswer;

    @NotNull(message = "Thời gian timeout không được để trống")
    @Positive(message = "Thời gian phải lớn hơn 0")
    @Schema(description = "Thời gian tối đa để trả lời (giây). Thường 30s cho trắc nghiệm, 60s cho phát âm.",
            example = "30")
    private Integer timeoutSeconds;

    @DecimalMin(value = "0.0", message = "Difficulty index phải >= 0")
    @DecimalMax(value = "1.0", message = "Difficulty index phải <= 1")
    @Schema(description = "Độ khó câu hỏi theo lý thuyết IRT: 0.0 (dễ nhất) → 1.0 (khó nhất). Để trống = hệ thống tự tính sau.",
            example = "0.5")
    private BigDecimal difficultyIndex;

    @Schema(description = "Trạng thái kích hoạt. Mặc định true khi tạo mới.", example = "true")
    private Boolean isActive;
}
