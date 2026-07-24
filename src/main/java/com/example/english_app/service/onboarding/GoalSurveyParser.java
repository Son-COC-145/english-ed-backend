package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.entity.enums.TopicCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoalSurveyParser {

    private final ObjectMapper objectMapper;

    public GoalSurveyRequest parse(String goalSurveyJson) {
        if (goalSurveyJson == null || goalSurveyJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(goalSurveyJson, GoalSurveyRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse goal_survey_json", e);
            return null;
        }
    }

    public List<TopicCategory> extractCategories(String goalSurveyJson) {
        GoalSurveyRequest request = parse(goalSurveyJson);
        if (request == null || request.getLearningPurpose() == null) {
            return List.of(TopicCategory.DAILY_CONVERSATION); // fallback
        }

        String purpose = request.getLearningPurpose().toLowerCase();
        List<TopicCategory> categories = new ArrayList<>();

        if (purpose.contains("công việc") || purpose.contains("đi làm") || purpose.contains("work")) {
            categories.add(TopicCategory.WORK);
        }
        if (purpose.contains("du lịch") || purpose.contains("travel")) {
            categories.add(TopicCategory.TRAVEL);
        }
        if (purpose.contains("ielts") || purpose.contains("thi") || purpose.contains("exam")) {
            categories.add(TopicCategory.EXAM_IELTS);
        }
        if (purpose.contains("du học") || purpose.contains("abroad") || purpose.contains("study abroad")) {
            categories.add(TopicCategory.STUDY_ABROAD);
        }
        if (purpose.contains("giao tiếp") || purpose.contains("hàng ngày") || purpose.contains("daily")) {
            categories.add(TopicCategory.DAILY_CONVERSATION);
        }

        // If no match, provide a fallback
        if (categories.isEmpty()) {
            categories.add(TopicCategory.DAILY_CONVERSATION);
        }
        
        return categories;
    }

    public List<String> extractFocusSkills(String goalSurveyJson) {
        GoalSurveyRequest request = parse(goalSurveyJson);
        if (request == null || request.getFocusSkills() == null || request.getFocusSkills().isEmpty()) {
            return Collections.emptyList();
        }
        return request.getFocusSkills();
    }
}
