package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementPronunciationAnswerResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.question.Question;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlacementTestServiceTest {

    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private PlacementTestAnswerRepository answerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementResultFactory resultFactory;
    @Mock private PlacementQuestionContentMapper questionContentMapper;
    @Mock private PlacementSessionExpiryService expiryService;
    @Mock private RoadmapJobService roadmapJobService;

    @InjectMocks
    private PlacementTestService service;

    private PlacementTestSession session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxPlacementQuestions", 20);
        session = PlacementTestSession.builder()
                .id(100L)
                .student(User.builder().id(1L).build())
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .currentQuestionIndex(0)
                .isCompleted(false)
                .build();
    }

    @Test
    void getNextQuestion_returnsSameAssignedQuestionOnRetry() {
        Question assigned = question(10L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        session.setCurrentQuestionId(10L);
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(assigned));

        PlacementQuestionResponse response = service.getNextQuestion(100L, 1L);

        assertThat(response.getQuestionId()).isEqualTo(10L);
        verify(questionRepository, never()).findBestAvailableForPlacement(anyLong(), anyString(), anyInt());
    }

    @Test
    void getNextQuestion_assignsQuestionWithSingleSelectionQuery() {
        Question selected = question(10L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findBestAvailableForPlacement(100L, "VOCABULARY", 1))
                .thenReturn(Optional.of(selected));

        PlacementQuestionResponse response = service.getNextQuestion(100L, 1L);

        assertThat(response.getQuestionId()).isEqualTo(10L);
        assertThat(session.getCurrentQuestionId()).isEqualTo(10L);
        verify(questionRepository).findBestAvailableForPlacement(100L, "VOCABULARY", 1);
    }

    @Test
    void submitAnswer_rejectsQuestionThatWasNotIssued() {
        session.setCurrentQuestionId(10L);
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));

        PlacementAnswerRequest request = new PlacementAnswerRequest(UUID.randomUUID(), 100L, 11L, "A", 1000);

        assertThatThrownBy(() -> service.submitAnswer(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_QUESTION_MISMATCH));

        verify(answerRepository, never()).save(any());
    }

    @Test
    void submitAnswer_advancesBlueprintAndAssignsNextQuestionAtomically() {
        Question current = question(10L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        Question next = question(20L, Skill.GRAMMAR, QuestionType.MULTIPLE_CHOICE, "B");
        session.setCurrentQuestionId(10L);
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(current));
        when(questionRepository.findBestAvailableForPlacement(100L, "GRAMMAR", 1))
                .thenReturn(Optional.of(next));

        PlacementQuestionResponse response = service.submitAnswer(
                1L, new PlacementAnswerRequest(UUID.randomUUID(), 100L, 10L, "A", 1000));

        assertThat(response.getQuestionId()).isEqualTo(20L);
        assertThat(response.getPreviousAnswerCorrect()).isTrue();
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(session.getCurrentQuestionId()).isEqualTo(20L);
        assertThat(session.getVocabCefrEstimate()).isEqualTo(CefrLevel.B1);

        ArgumentCaptor<PlacementTestAnswer> answer = ArgumentCaptor.forClass(PlacementTestAnswer.class);
        verify(answerRepository).save(answer.capture());
        assertThat(answer.getValue().getQuestion().getId()).isEqualTo(10L);
        assertThat(answer.getValue().getIsCorrect()).isTrue();
    }

    @Test
    void submitAnswer_sameSubmissionIdReplaysWithoutAdvancingAgain() {
        UUID submissionId = UUID.randomUUID();
        PlacementAnswerRequest request = new PlacementAnswerRequest(
                submissionId, 100L, 10L, "A", 1000);
        Question answeredQuestion = question(
                10L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        Question currentQuestion = question(
                20L, Skill.GRAMMAR, QuestionType.MULTIPLE_CHOICE, "B");
        session.setCurrentQuestionIndex(1);
        session.setCurrentQuestionId(20L);
        PlacementTestAnswer persisted = PlacementTestAnswer.builder()
                .session(session)
                .question(answeredQuestion)
                .submissionId(submissionId)
                .requestHash(PlacementSubmissionHasher.answer(100L, 10L, "A", 1000))
                .submissionType("ANSWER")
                .isCorrect(true)
                .build();

        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(answerRepository.findReplayCandidates(100L, submissionId, 10L))
                .thenReturn(List.of(persisted));
        when(questionRepository.findById(20L)).thenReturn(Optional.of(currentQuestion));

        PlacementQuestionResponse response = service.submitAnswer(1L, request);

        assertThat(response.getQuestionId()).isEqualTo(20L);
        assertThat(response.getSubmittedQuestionId()).isEqualTo(10L);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
        verify(answerRepository, never()).save(any());
        verify(questionRepository, never()).findBestAvailableForPlacement(anyLong(), anyString(), anyInt());
    }

    @Test
    void submitAnswer_reusedSubmissionIdWithDifferentPayloadIsRejected() {
        UUID submissionId = UUID.randomUUID();
        Question answeredQuestion = question(
                10L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        PlacementTestAnswer persisted = PlacementTestAnswer.builder()
                .session(session)
                .question(answeredQuestion)
                .submissionId(submissionId)
                .requestHash(PlacementSubmissionHasher.answer(100L, 10L, "A", 1000))
                .submissionType("ANSWER")
                .build();
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(answerRepository.findReplayCandidates(100L, submissionId, 10L))
                .thenReturn(List.of(persisted));

        PlacementAnswerRequest conflicting = new PlacementAnswerRequest(
                submissionId, 100L, 10L, "B", 1000);

        assertThatThrownBy(() -> service.submitAnswer(1L, conflicting))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED));
        verify(answerRepository, never()).save(any());
    }

    @Test
    void textSubmitEndpoint_rejectsPronunciationQuestion() {
        session.setCurrentQuestionIndex(4);
        session.setCurrentQuestionId(50L);
        Question pronunciation = question(50L, Skill.PRONUNCIATION, QuestionType.PRONUNCIATION, "hello");
        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(50L)).thenReturn(Optional.of(pronunciation));

        assertThatThrownBy(() -> service.submitAnswer(
                1L, new PlacementAnswerRequest(UUID.randomUUID(), 100L, 50L, "hello", 1000)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_QUESTION_MISMATCH));

        verify(answerRepository, never()).save(any());
    }

    @Test
    void pronunciationAssessment_usesServerAnswerAndContinuesAtNextBlueprintSkill() {
        session.setCurrentQuestionIndex(4);
        session.setCurrentQuestionId(50L);
        Question pronunciation = question(50L, Skill.PRONUNCIATION, QuestionType.PRONUNCIATION, "hello");
        Question next = question(60L, Skill.VOCABULARY, QuestionType.MULTIPLE_CHOICE, "A");
        PronunciationScoreResult score = PronunciationScoreResult.builder()
                .overallScore((short) 75).status("SCORED").build();

        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(50L)).thenReturn(Optional.of(pronunciation));
        when(questionRepository.findBestAvailableForPlacement(100L, "VOCABULARY", 1))
                .thenReturn(Optional.of(next));

        PlacementPronunciationAnswerResponse response = service.recordPronunciationAssessment(
                1L, 100L, 50L, UUID.randomUUID(), "request-hash", score);

        assertThat(response.getNextQuestion().getQuestionId()).isEqualTo(60L);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(5);
        assertThat(session.getCurrentQuestionId()).isEqualTo(60L);

        ArgumentCaptor<PlacementTestAnswer> answer = ArgumentCaptor.forClass(PlacementTestAnswer.class);
        verify(answerRepository).save(answer.capture());
        assertThat(answer.getValue().getAnswerGiven()).isEqualTo("hello");
        assertThat(answer.getValue().getIsCorrect()).isTrue();
    }

    @Test
    void startTest_failsBeforeCreatingSessionWhenAnySkillCannotSupplyFourQuestions() {
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        when(sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(1L))
                .thenReturn(Optional.empty());
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(session.getStudent()));
        when(questionRepository.countPlacementReadyGroupBySkill()).thenReturn(List.of(
                new Object[]{Skill.VOCABULARY, 4L},
                new Object[]{Skill.GRAMMAR, 4L},
                new Object[]{Skill.READING, 4L},
                new Object[]{Skill.LISTENING, 3L},
                new Object[]{Skill.PRONUNCIATION, 4L}
        ));

        assertThatThrownBy(() -> service.startTest(1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_QUESTION_EXHAUSTED));

        verify(sessionRepository, never()).save(any());
    }

    @Test
    void twentiethPronunciationAnswer_completesWithExactlyTwentyAnswersAndNoNextQuery() {
        session.setCurrentQuestionIndex(19);
        session.setCurrentQuestionId(50L);
        Question pronunciation = question(50L, Skill.PRONUNCIATION, QuestionType.PRONUNCIATION, "hello");
        PronunciationScoreResult score = PronunciationScoreResult.builder()
                .overallScore((short) 75).status("SCORED").build();
        PlacementResultFactory.SkillScores scores = PlacementResultFactory.SkillScores.builder()
                .vocab((short) 50).grammar((short) 50).reading((short) 50)
                .listening((short) 50).pronunciation((short) 50).build();
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .student(session.getStudent()).goalSurveyJson("{}").build();
        PlacementResultResponse result = PlacementResultResponse.builder().cefrLevel("B1").build();

        when(sessionRepository.findByIdWithStudentForUpdate(100L)).thenReturn(Optional.of(session));
        when(questionRepository.findById(50L)).thenReturn(Optional.of(pronunciation));
        when(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).thenReturn(
                Collections.nCopies(20, PlacementTestAnswer.builder().build()));
        when(resultFactory.calculateAllSkills(anyList())).thenReturn(scores);
        when(resultFactory.calculateFinalCefrMedian(anyMap())).thenReturn(CefrLevel.B1);
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        when(resultFactory.buildResponse(eq(CefrLevel.B1), anyMap(), eq(scores), anyList(),
                isNull(), eq(false))).thenReturn(result);

        PlacementPronunciationAnswerResponse response = service.recordPronunciationAssessment(
                1L, 100L, 50L, UUID.randomUUID(), "request-hash", score);

        assertThat(response.isTestCompleted()).isTrue();
        assertThat(response.getPlacementResult()).isSameAs(result);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(20);
        assertThat(session.getIsCompleted()).isTrue();
        assertThat(session.getCurrentQuestionId()).isNull();
        verify(questionRepository, never()).findBestAvailableForPlacement(anyLong(), anyString(), anyInt());
    }

    private Question question(Long id, Skill skill, QuestionType type, String correctAnswer) {
        return Question.builder()
                .id(id)
                .cefrLevel(CefrLevel.A2)
                .skill(skill)
                .questionType(type)
                .contentJson("{}")
                .correctAnswer(correctAnswer)
                .timeoutSeconds(30)
                .isActive(true)
                .build();
    }
}
