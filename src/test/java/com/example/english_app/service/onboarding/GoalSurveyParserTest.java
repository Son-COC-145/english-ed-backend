package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.TopicCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GoalSurveyParserTest {

    private GoalSurveyParser parser;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        parser = new GoalSurveyParser(objectMapper);
    }

    @Test
    void extractCategories_ShouldMapWorkCorrectly() {
        String json = "{\"learningPurpose\": \"Phục vụ công việc\"}";
        List<TopicCategory> categories = parser.extractCategories(json);
        assertTrue(categories.contains(TopicCategory.WORK));
    }

    @Test
    void extractCategories_ShouldMapTravelCorrectly() {
        String json = "{\"learningPurpose\": \"Đi du lịch\"}";
        List<TopicCategory> categories = parser.extractCategories(json);
        assertTrue(categories.contains(TopicCategory.TRAVEL));
    }
    
    @Test
    void extractCategories_ShouldHandleNullJson() {
        List<TopicCategory> categories = parser.extractCategories(null);
        assertNotNull(categories);
        assertTrue(categories.contains(TopicCategory.DAILY_CONVERSATION)); // Default
    }

    @Test
    void extractFocusSkills_ShouldExtractCorrectly() {
        String json = "{\"focusSkills\": [\"Giao tiếp\", \"Nghe hiểu\"]}";
        List<String> skills = parser.extractFocusSkills(json);
        assertEquals(2, skills.size());
        assertTrue(skills.contains("Giao tiếp"));
        assertTrue(skills.contains("Nghe hiểu"));
    }
    
    @Test
    void extractFocusSkills_ShouldHandleNullOrEmpty() {
        List<String> skills = parser.extractFocusSkills(null);
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
        
        skills = parser.extractFocusSkills("{}");
        assertNotNull(skills);
        assertTrue(skills.isEmpty());
    }
}
