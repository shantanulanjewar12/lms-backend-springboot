package com.lms.enrollment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.repository.LessonRepository;
import com.lms.enrollment.dto.request.LessonProgressRequestDto;
import com.lms.enrollment.dto.response.ProgressResponseDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.service.BadgeService;
import com.lms.notification.vo.BadgeType;
import com.lms.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class ProgressServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private BadgeService badgeService;

    @Mock
    private StudentBadgeRepository badgeRepository;

    @Mock
    private LearningStreakRepository streakRepository;

    @InjectMocks
    private ProgressServiceImpl progressService;

    private Enrollment enrollment;
    private Lesson lesson;

    @BeforeEach
    void setup() {
        Course course = Course.builder()
                .id(101L)
                .build();

        lesson = Lesson.builder()
                .id(1L)
                .course(course)
                .build();

        enrollment = Enrollment.builder()
                .id(10L)
                .studentId(5L)
                .courseId(101L)
                .status(EnrollmentStatus.ACTIVE)
                .build();
    }

    @Test
    void updateLessonProgress_shouldSaveProgress_andReturnResponse() {
        LessonProgressRequestDto request = new LessonProgressRequestDto();
        request.setLessonId(1L);
        request.setIsCompleted(true);
        request.setWatchDurationSeconds(120);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(10L, 1L))
                .thenReturn(Optional.empty());
        when(lessonRepository.countByCourseId(101L)).thenReturn(5L);
        when(lessonProgressRepository.findByEnrollmentId(10L))
                .thenReturn(List.of(
                        LessonProgress.builder()
                                .lessonId(1L)
                                .isCompleted(true)
                                .watchDurationSeconds(120)
                                .build()
                ));
        when(streakRepository.findByStudentId(5L)).thenReturn(Optional.empty());
        when(lessonProgressRepository.findAll()).thenReturn(List.of());

        ProgressResponseDto result = progressService.updateLessonProgress(5L, request);

        assertNotNull(result);
        assertEquals(20.0, result.getCompletionPercentage());
        verify(lessonProgressRepository).save(any(LessonProgress.class));
    }

    @Test
    void updateLessonProgress_shouldAwardFirstLessonBadge() {
        LessonProgressRequestDto request = new LessonProgressRequestDto();
        request.setLessonId(1L);
        request.setIsCompleted(true);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(10L, 1L))
                .thenReturn(Optional.empty());
        when(lessonProgressRepository.findAll()).thenReturn(List.of(
                LessonProgress.builder()
                        .enrollmentId(10L)
                        .lessonId(1L)
                        .isCompleted(true)
                        .build()
        ));
        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(badgeRepository.existsByStudentIdAndBadgeType(5L, BadgeType.FIRST_LESSON))
                .thenReturn(false);
        when(lessonRepository.countByCourseId(101L)).thenReturn(5L);
        when(lessonProgressRepository.findByEnrollmentId(10L))
                .thenReturn(List.of(
                        LessonProgress.builder()
                                .lessonId(1L)
                                .isCompleted(true)
                                .build()
                ));
        when(streakRepository.findByStudentId(5L)).thenReturn(Optional.empty());

        progressService.updateLessonProgress(5L, request);

        verify(badgeService).awardBadge(5L, BadgeType.FIRST_LESSON);
    }

    @Test
    void updateLessonProgress_shouldMarkCourseCompleted_andAwardBadge() {
        LessonProgressRequestDto request = new LessonProgressRequestDto();
        request.setLessonId(1L);
        request.setIsCompleted(true);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(10L, 1L))
                .thenReturn(Optional.empty());
        when(lessonRepository.countByCourseId(101L)).thenReturn(3L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(10L))
                .thenReturn(3L);
        when(lessonProgressRepository.findByEnrollmentId(10L))
                .thenReturn(List.of(
                        LessonProgress.builder().lessonId(1L).isCompleted(true).build(),
                        LessonProgress.builder().lessonId(2L).isCompleted(true).build(),
                        LessonProgress.builder().lessonId(3L).isCompleted(true).build()
                ));
        when(badgeRepository.existsByStudentIdAndBadgeType(5L, BadgeType.COURSE_COMPLETE))
                .thenReturn(false);
        when(streakRepository.findByStudentId(5L)).thenReturn(Optional.empty());
        when(lessonProgressRepository.findAll()).thenReturn(List.of());

        progressService.updateLessonProgress(5L, request);

        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(badgeService).awardBadge(5L, BadgeType.COURSE_COMPLETE);
    }

    @Test
    void getCourseProgress_shouldReturnProgress() {
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.of(enrollment));
        when(lessonRepository.countByCourseId(101L)).thenReturn(4L);
        when(lessonProgressRepository.findByEnrollmentId(10L))
                .thenReturn(List.of(
                        LessonProgress.builder().lessonId(1L).isCompleted(true).build(),
                        LessonProgress.builder().lessonId(2L).isCompleted(false).build()
                ));

        ProgressResponseDto result = progressService.getCourseProgress(5L, 101L);

        assertNotNull(result);
        assertEquals(25.0, result.getCompletionPercentage());
    }

    @Test
    void getCourseProgress_shouldThrowException_whenEnrollmentNotFound() {
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> progressService.getCourseProgress(5L, 101L));
    }

    @Test
    void updateLessonProgress_shouldUpdateStreak() {
        LessonProgressRequestDto request = new LessonProgressRequestDto();
        request.setLessonId(1L);
        request.setIsCompleted(true);

        when(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson));
        when(enrollmentRepository.findByStudentIdAndCourseId(5L, 101L))
                .thenReturn(Optional.of(enrollment));
        when(lessonProgressRepository.findByEnrollmentIdAndLessonId(10L, 1L))
                .thenReturn(Optional.empty());

        LearningStreak streak = LearningStreak.builder()
                .studentId(5L)
                .currentStreak(2)
                .longestStreak(2)
                .lastActiveDate(LocalDate.now().minusDays(1))
                .build();

        when(streakRepository.findByStudentId(5L)).thenReturn(Optional.of(streak));
        when(lessonRepository.countByCourseId(101L)).thenReturn(5L);
        when(lessonProgressRepository.findByEnrollmentId(10L))
                .thenReturn(List.of(
                        LessonProgress.builder().lessonId(1L).isCompleted(true).build()
                ));
        when(lessonProgressRepository.findAll()).thenReturn(List.of());

        progressService.updateLessonProgress(5L, request);

        verify(streakRepository).save(any(LearningStreak.class));
    }
}