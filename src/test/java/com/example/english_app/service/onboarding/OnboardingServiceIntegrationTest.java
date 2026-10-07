package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.adaptive.event.LearningEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementTestServiceIntegrationTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private PlacementTestAnswerRepository answerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementResultFactory resultFactory;
    @Mock private PlacementQuestionContentMapper questionContentMapper;
    @Mock private PlacementSessionExpiryService expiryService;
    @Mock private RoadmapJobService roadmapJobService;
    @Mock private LearningEventOutboxService learningEventOutboxService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PlacementTestService placementTestService;

    private PlacementTestSession session;
    private StudentOnboarding onboarding;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(placementTestService, "maxPlacementQuestions", 20);
        User user = User.builder().id(1L).build();
        session = PlacementTestSession.builder()
                .id(100L)
                .student(user)
                .isCompleted(false)
                .currentQuestionIndex(20)
                .build();
        onboarding = StudentOnboarding.builder()
                .student(user)
                .goalSurveyJson("{}")
                .build();
    }

    @Test
    void completeTest_requiresExactlyTwentyPersistedAnswers() {
        session.setCurrentQuestionIndex(19);
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).thenReturn(
                Collections.nCopies(19, PlacementTestAnswer.builder().build()));

        assertThatThrownBy(() -> placementTestService.completeTest(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_INCOMPLETE));

        verify(onboardingRepository, never()).save(any());
        assertThat(session.getIsCompleted()).isFalse();
    }

    @Test
    void completeTest_persistsResultAndQueuesDurableRoadmapJob() {
        PlacementResultFactory.SkillScores scores = PlacementResultFactory.SkillScores.builder()
                .vocab((short) 50).grammar((short) 50).reading((short) 50)
                .listening((short) 50).pronunciation((short) 50).build();
        PlacementResultResponse expected = PlacementResultResponse.builder()
                .cefrLevel("B1").roadmapGenerated(false).build();

        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L))
                .thenReturn(Collections.nCopies(20, PlacementTestAnswer.builder().build()));
        when(resultFactory.calculateAllSkills(anyList())).thenReturn(scores);
        when(resultFactory.calculateFinalCefrMedian(anyMap())).thenReturn(CefrLevel.B1);
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        when(resultFactory.buildResponse(eq(CefrLevel.B1), anyMap(), eq(scores), anyList(),
                isNull(), eq(false))).thenReturn(expected);

        PlacementResultResponse result = placementTestService.completeTest(100L, 1L);

        assertThat(result).isSameAs(expected);
        assertThat(session.getIsCompleted()).isTrue();
        verify(onboardingRepository).save(onboarding);
        verify(roadmapJobService).enqueue(1L, 1, CefrLevel.B1, "{}");
        ArgumentCaptor<LearningEventRequest> event = ArgumentCaptor.forClass(LearningEventRequest.class);
        verify(learningEventOutboxService).saveOutbox(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(LearningEventType.PLACEMENT_COMPLETED);
        assertThat(event.getValue().getSource()).isEqualTo(LearningEventSource.PLACEMENT_SESSION);
        assertThat(event.getValue().getSourceReference()).isEqualTo("PLACEMENT:1");
        assertThat(event.getValue().getPayload().path("overallCefr").asText()).isEqualTo("B1");
        assertThat(event.getValue().getPayload().path("skills").size()).isEqualTo(5);
    }
}
