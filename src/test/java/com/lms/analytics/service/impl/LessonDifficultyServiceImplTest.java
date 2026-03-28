package com.lms.analytics.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.analytics.dto.response.LessonDifficultyResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.repository.LessonRepository;
import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.LearningEventType;

@ExtendWith(MockitoExtension.class)
class LessonDifficultyServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private LessonDifficultyServiceImpl lessonDifficultyService;

    private Lesson lesson1;
    private Lesson lesson2;

    @BeforeEach
    void setUp() {
        Course course = Course.builder()
                .id(1L)
                .title("Java Course")
                .build();

        lesson1 = Lesson.builder()
                .id(101L)
                .course(course)
                .title("Intro to Java")
                .orderIndex(1)
                .build();

        lesson2 = Lesson.builder()
                .id(102L)
                .course(course)
                .title("Advanced Java")
                .orderIndex(2)
                .build();
    }

    @Test
    void getDifficultyForCourse_shouldReturnDifficultyList() {
        when(lessonRepository.findByCourseIdOrderByOrderIndex(1L))
                .thenReturn(List.of(lesson1, lesson2));

        when(learningEventRepository.findByLessonId(101L))
                .thenReturn(List.of(
                        createEvent(1L, 101L, LearningEventType.QUIZ_ATTEMPT, BigDecimal.ONE),
                        createEvent(2L, 101L, LearningEventType.QUIZ_ATTEMPT, BigDecimal.ONE),
                        createEvent(1L, 101L, LearningEventType.VIDEO_WATCH, BigDecimal.ONE),
                        createEvent(2L, 101L, LearningEventType.VIDEO_WATCH, BigDecimal.ONE)
                ));
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.QUIZ_ATTEMPT))
                .thenReturn(4L);
        when(learningEventRepository.countByLessonIdAndEventTypeAndValueGreaterThanEqual(
                101L, LearningEventType.QUIZ_SCORE, BigDecimal.valueOf(50)))
                .thenReturn(3L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_WATCH))
                .thenReturn(10L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_REWIND))
                .thenReturn(2L);
        when(lessonProgressRepository.countByLessonIdAndIsCompletedTrue(101L))
                .thenReturn(1L);

        when(learningEventRepository.findByLessonId(102L))
                .thenReturn(List.of(
                        createEvent(1L, 102L, LearningEventType.QUIZ_ATTEMPT, BigDecimal.ONE),
                        createEvent(2L, 102L, LearningEventType.QUIZ_ATTEMPT, BigDecimal.ONE),
                        createEvent(3L, 102L, LearningEventType.VIDEO_WATCH, BigDecimal.ONE)
                ));
        when(learningEventRepository.countByLessonIdAndEventType(102L, LearningEventType.QUIZ_ATTEMPT))
                .thenReturn(2L);
        when(learningEventRepository.countByLessonIdAndEventTypeAndValueGreaterThanEqual(
                102L, LearningEventType.QUIZ_SCORE, BigDecimal.valueOf(50)))
                .thenReturn(0L);
        when(learningEventRepository.countByLessonIdAndEventType(102L, LearningEventType.VIDEO_WATCH))
                .thenReturn(4L);
        when(learningEventRepository.countByLessonIdAndEventType(102L, LearningEventType.VIDEO_REWIND))
                .thenReturn(2L);
        when(lessonProgressRepository.countByLessonIdAndIsCompletedTrue(102L))
                .thenReturn(1L);

        List<LessonDifficultyResponseDto> result = lessonDifficultyService.getDifficultyForCourse(1L);

        assertNotNull(result);
        assertEquals(2, result.size());

        LessonDifficultyResponseDto dto1 = result.get(0);
        assertEquals(101L, dto1.getLessonId());
        assertEquals("Intro to Java", dto1.getLessonTitle());
        assertEquals(0.285, dto1.getDifficultyScore(), 0.0001);
        assertEquals(0.25, dto1.getQuizFailureRate(), 0.0001);
        assertEquals(0.2, dto1.getRewindRate(), 0.0001);
        assertEquals(0.5, dto1.getDropOffRate(), 0.0001);

        LessonDifficultyResponseDto dto2 = result.get(1);
        assertEquals(102L, dto2.getLessonId());
        assertEquals("Advanced Java", dto2.getLessonTitle());
        assertEquals(0.7833333333, dto2.getDifficultyScore(), 0.0001);
        assertEquals(1.0, dto2.getQuizFailureRate(), 0.0001);
        assertEquals(0.5, dto2.getRewindRate(), 0.0001);
        assertEquals(0.6666666667, dto2.getDropOffRate(), 0.0001);
    }

    @Test
    void getDifficultyForCourse_shouldReturnZeroRates_whenNoAttemptsOrViewsOrStarts() {
        when(lessonRepository.findByCourseIdOrderByOrderIndex(1L))
                .thenReturn(List.of(lesson1));

        when(learningEventRepository.findByLessonId(101L))
                .thenReturn(List.of());
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.QUIZ_ATTEMPT))
                .thenReturn(0L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_WATCH))
                .thenReturn(0L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_REWIND))
                .thenReturn(0L);
        when(lessonProgressRepository.countByLessonIdAndIsCompletedTrue(101L))
                .thenReturn(0L);

        List<LessonDifficultyResponseDto> result = lessonDifficultyService.getDifficultyForCourse(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        LessonDifficultyResponseDto dto = result.get(0);
        assertEquals(101L, dto.getLessonId());
        assertEquals("Intro to Java", dto.getLessonTitle());
        assertEquals(0.0, dto.getDifficultyScore(), 0.0001);
        assertEquals(0.0, dto.getQuizFailureRate(), 0.0001);
        assertEquals(0.0, dto.getRewindRate(), 0.0001);
        assertEquals(0.0, dto.getDropOffRate(), 0.0001);
    }

    @Test
    void getDifficultyForCourse_shouldHandleCompletedGreaterThanStarted() {
        when(lessonRepository.findByCourseIdOrderByOrderIndex(1L))
                .thenReturn(List.of(lesson1));

        when(learningEventRepository.findByLessonId(101L))
                .thenReturn(List.of(
                        createEvent(1L, 101L, LearningEventType.VIDEO_WATCH, BigDecimal.ONE)
                ));
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.QUIZ_ATTEMPT))
                .thenReturn(0L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_WATCH))
                .thenReturn(1L);
        when(learningEventRepository.countByLessonIdAndEventType(101L, LearningEventType.VIDEO_REWIND))
                .thenReturn(0L);
        when(lessonProgressRepository.countByLessonIdAndIsCompletedTrue(101L))
                .thenReturn(5L);

        List<LessonDifficultyResponseDto> result = lessonDifficultyService.getDifficultyForCourse(1L);

        assertNotNull(result);
        assertEquals(1, result.size());

        LessonDifficultyResponseDto dto = result.get(0);
        assertEquals(0.0, dto.getDropOffRate(), 0.0001);
        assertEquals(0.0, dto.getDifficultyScore(), 0.0001);
    }

    private LearningEvent createEvent(Long studentId, Long lessonId, LearningEventType eventType, BigDecimal value) {
        return LearningEvent.builder()
                .studentId(studentId)
                .lessonId(lessonId)
                .eventType(eventType)
                .value(value)
                .build();
    }
}