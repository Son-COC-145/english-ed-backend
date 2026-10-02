package com.example.english_app.controller;

import com.example.english_app.exception.GlobalExceptionHandler;
import com.example.english_app.service.onboarding.OnboardingLifecycleService;
import com.example.english_app.service.onboarding.PlacementTestService;
import com.example.english_app.service.onboarding.PronunciationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OnboardingGoalSurveyContractTest {

    @Mock private OnboardingLifecycleService lifecycleService;
    @Mock private PlacementTestService placementTestService;
    @Mock private PronunciationService pronunciationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OnboardingController controller = new OnboardingController(
                lifecycleService, placementTestService, pronunciationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsUnknownLearningGoalCodeAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/onboarding/goal-survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson().replace("COMMUNICATION", "FREE_TEXT_VALUE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4001));

        verifyNoInteractions(lifecycleService);
    }

    @Test
    void rejectsMoreThanThreeFocusSkillCodesAsBadRequest() throws Exception {
        String json = validJson().replace(
                "[\"SPEAKING\",\"PRONUNCIATION\"]",
                "[\"SPEAKING\",\"PRONUNCIATION\",\"LISTENING\",\"READING\"]");

        mockMvc.perform(post("/api/v1/onboarding/goal-survey")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4000));

        verifyNoInteractions(lifecycleService);
    }

    private String validJson() {
        return """
                {
                  "learningGoal":"COMMUNICATION",
                  "focusSkills":["SPEAKING","PRONUNCIATION"],
                  "dailyStudyMinutes":30,
                  "preferredEnvironment":"ONLINE",
                  "previousExperience":"BEGINNER"
                }
                """;
    }
}
