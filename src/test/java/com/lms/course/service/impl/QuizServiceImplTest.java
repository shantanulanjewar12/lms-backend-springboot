package com.lms.course.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.course.dto.request.QuizAnswerItemRequestDto;
import com.lms.course.dto.request.QuizQuestionUpsertRequestDto;
import com.lms.course.dto.request.QuizSubmitRequestDto;
import com.lms.course.dto.response.QuizQuestionResponseDto;
import com.lms.course.dto.response.QuizSubmitResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.QuizQuestion;
import com.lms.course.repository.QuizQuestionRepository;
import com.lms.course.service.LessonService;
import com.lms.course.vo.ContentType;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class QuizServiceImplTest {

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private LessonService lessonService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LearningEventRepository learningEventRepository;

    @InjectMocks
    private QuizServiceImpl quizService;

    private Lesson lesson;
    private Course course;

    @BeforeEach
    void setup() {
        course = Course.builder()
                .id(1L)
                .instructorId(100L)
                .build();

        lesson = Lesson.builder()
                .id(10L)
                .course(course)
                .contentType(ContentType.QUIZ)
                .isFreePreview(false)
                .build();
    }

    // ========================
    // UPSERT TEST
    // ========================

    @Test
    void upsertQuizQuestions_shouldSaveQuestions() {
        QuizQuestionUpsertRequestDto req = new QuizQuestionUpsertRequestDto();
        req.setQuestionText("Q1");
        req.setOptionA("A");
        req.setOptionB("B");
        req.setOptionC("C");
        req.setOptionD("D");
        req.setCorrectOptions(List.of("A"));
        req.setOrderIndex(1);

        QuizQuestion savedQuestion = QuizQuestion.builder()
                .id(1L)
                .lesson(lesson)
                .questionText("Q1")
                .optionA("A")
                .optionB("B")
                .optionC("C")
                .optionD("D")
                .correctOptions("A")
                .orderIndex(1)
                .build();

        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of(savedQuestion));

        List<QuizQuestionResponseDto> result =
                quizService.upsertQuizQuestions(100L, 10L, List.of(req));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getLessonId());
        assertEquals("Q1", result.get(0).getQuestionText());
        assertEquals(List.of("A"), result.get(0).getCorrectOptions());

        verify(quizQuestionRepository).deleteByLessonId(10L);
        verify(quizQuestionRepository).saveAll(any());
    }

    @Test
    void upsertQuizQuestions_shouldThrow_whenEmptyRequest() {
        assertThrows(BadRequestException.class,
                () -> quizService.upsertQuizQuestions(100L, 10L, List.of()));
    }

    @Test
    void upsertQuizQuestions_shouldThrow_whenUnauthorized() {
        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);

        assertThrows(UnauthorizedException.class,
                () -> quizService.upsertQuizQuestions(999L, 10L, List.of(new QuizQuestionUpsertRequestDto())));
    }

    // ========================
    // GET QUESTIONS
    // ========================

    @Test
    void getQuizQuestions_shouldHideAnswers_forStudent() {
        QuizQuestion q = QuizQuestion.builder()
                .id(1L)
                .lesson(lesson)
                .questionText("Q1")
                .optionA("A")
                .optionB("B")
                .optionC("C")
                .optionD("D")
                .correctOptions("A")
                .orderIndex(1)
                .build();

        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(enrollmentRepository.existsByStudentIdAndCourseId(200L, 1L)).thenReturn(true);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of(q));

        List<QuizQuestionResponseDto> result =
                quizService.getQuizQuestions(10L, 200L, "STUDENT");

        assertNull(result.get(0).getCorrectOptions()); // hidden
    }

    @Test
    void getQuizQuestions_shouldShowAnswers_forInstructor() {
        QuizQuestion q = QuizQuestion.builder()
                .id(1L)
                .lesson(lesson)
                .questionText("Q1")
                .correctOptions("A")
                .build();

        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of(q));

        List<QuizQuestionResponseDto> result =
                quizService.getQuizQuestions(10L, 100L, "INSTRUCTOR");

        assertNotNull(result.get(0).getCorrectOptions());
    }

    // ========================
    // SUBMIT QUIZ
    // ========================

    @Test
    void submitQuiz_shouldCalculateScoreCorrectly() {
        QuizQuestion q = QuizQuestion.builder()
                .id(1L)
                .lesson(lesson)
                .correctOptions("A")
                .orderIndex(1)
                .build();

        QuizAnswerItemRequestDto answer = new QuizAnswerItemRequestDto();
        answer.setQuestionId(1L);
        answer.setSelectedOptions(List.of("A"));

        QuizSubmitRequestDto request = new QuizSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(enrollmentRepository.existsByStudentIdAndCourseId(200L, 1L)).thenReturn(true);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of(q));

        QuizSubmitResponseDto result =
                quizService.submitQuiz(200L, 10L, request);

        assertEquals(1, result.getCorrectAnswers());
        assertEquals(100.0, result.getScorePercent());
        assertTrue(result.getPassed());
    }

    @Test
    void submitQuiz_shouldFail_whenWrongAnswer() {
        QuizQuestion q = QuizQuestion.builder()
                .id(1L)
                .lesson(lesson)
                .correctOptions("A")
                .orderIndex(1)
                .build();

        QuizAnswerItemRequestDto answer = new QuizAnswerItemRequestDto();
        answer.setQuestionId(1L);
        answer.setSelectedOptions(List.of("B"));

        QuizSubmitRequestDto request = new QuizSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(enrollmentRepository.existsByStudentIdAndCourseId(200L, 1L)).thenReturn(true);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of(q));

        QuizSubmitResponseDto result =
                quizService.submitQuiz(200L, 10L, request);

        assertEquals(0, result.getCorrectAnswers());
        assertFalse(result.getPassed());
    }

    @Test
    void submitQuiz_shouldThrow_whenNotEnrolled() {
        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(enrollmentRepository.existsByStudentIdAndCourseId(200L, 1L)).thenReturn(false);

        assertThrows(UnauthorizedException.class,
                () -> quizService.submitQuiz(200L, 10L, new QuizSubmitRequestDto()));
    }

    @Test
    void submitQuiz_shouldThrow_whenNoQuestions() {
        when(lessonService.getLessonEntityById(10L)).thenReturn(lesson);
        when(enrollmentRepository.existsByStudentIdAndCourseId(200L, 1L)).thenReturn(true);
        when(quizQuestionRepository.findByLessonIdOrderByOrderIndex(10L))
                .thenReturn(List.of());

        assertThrows(BadRequestException.class,
                () -> quizService.submitQuiz(200L, 10L, new QuizSubmitRequestDto()));
    }
}