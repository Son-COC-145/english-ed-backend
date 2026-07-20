package com.example.english_app.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AiVocabularyResponse {
    private String ipaTranscription;
    private String definitionVi;
    private String nuanceNote;
    private List<Map<String, String>> exampleSentencesJson;
    private List<Map<String, String>> collocationJson;
    private List<Map<String, String>> dialogueJson;
}
