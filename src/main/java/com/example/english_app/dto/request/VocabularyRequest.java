package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyRequest {

    @NotNull(message = "Vui lòng chọn chủ đề (Topic) cho từ vựng")
    private Short topicId;

    @NotBlank(message = "Không được để trống từ vựng")
    @Size(max = 150, message = "Từ vựng không được vượt quá 150 ký tự")
    private String word;

    @NotBlank(message = "Không được để trống phiên âm (IPA)")
    @Size(max = 300, message = "Phiên âm không được vượt quá 300 ký tự")
    private String ipaTranscription;

    @NotNull(message = "Vui lòng chọn trình độ CEFR")
    private CefrLevel cefrLevel;

    @NotBlank(message = "Không được để trống định nghĩa tiếng Việt")
    private String definitionVi;

    private String imageUrl;

    @NotBlank(message = "Không được để trống link audio phát âm (giọng Mỹ)")
    private String audioUsUrl;

    private String audioUkUrl;
    private String collocationJson;
    private String nuanceNote;
    private String exampleSentencesJson;
    private String dialogueJson;
    private VocabularyStatus status;
    private Long createdById;
    private LocalDateTime publishedAt;
}
