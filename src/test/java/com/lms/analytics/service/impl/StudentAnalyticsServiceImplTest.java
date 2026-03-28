package com.lms.analytics.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;

@ExtendWith(MockitoExtension.class)
class StudentAnalyticsServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LearningStreakRepository learningStreakRepository;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private StudentAnalyticsServiceImpl service;

    private Enrollment enrollment1;
    private Enrollment enrollment2;

    @BeforeEach
    void setUp() {
        enrollment1 = Enrollment.builder()
                .id(1L)
                .courseId(101L)
                .status(EnrollmentStatus.COMPLETED)
                .build();

        enrollment2 = Enrollment.builder()
                .id(2L)
                .courseId(102L)
                .status(EnrollmentStatus.ACTIVE)
                .build();
    }

    // ===========================
    // getStudentSummary()
    // ===========================

    @Test
    void getStudentSummary_shouldReturnCorrectSummary() {
        when(enrollmentRepository.findByStudentId(1L))
                .thenReturn(List.of(enrollment1, enrollment2));

        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(lessonRepository.countByCourseId(102L)).thenReturn(20L);

        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(10L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(2L)).thenReturn(10L);

        LearningStreak streak = new LearningStreak();
        streak.setCurrentStreak(5);
        when(learningStreakRepository.findByStudentId(1L)).thenReturn(Optional.of(streak));

        StudentProgressSummaryDto result = service.getStudentSummary(1L);

        assertNotNull(result);
        assertEquals(1L, result.getStudentId());
        assertEquals(2, result.getTotalEnrolled());
        assertEquals(1, result.getTotalCompleted());
        assertEquals(75.0, result.getAverageCompletionPercent(), 0.0001);
        assertEquals(5, result.getStreakDays());
    }

    @Test
    void getStudentSummary_shouldHandleEmptyEnrollments() {
        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of());
        when(learningStreakRepository.findByStudentId(1L)).thenReturn(Optional.empty());

        StudentProgressSummaryDto result = service.getStudentSummary(1L);

        assertEquals(0, result.getTotalEnrolled());
        assertEquals(0, result.getTotalCompleted());
        assertEquals(0.0, result.getAverageCompletionPercent());
        assertEquals(0, result.getStreakDays());
    }

    // ===========================
    // getLearningPath()
    // ===========================

    @Test
    void getLearningPath_shouldReturnRecommendations_forBackendGoal() {

        when(enrollmentRepository.findByStudentId(1L))
                .thenReturn(List.of(enrollment1)); // already enrolled course 101

        Course c1 = buildCourse(201L, "Spring Boot API", "Backend development", "Backend",
                CourseLevel.BEGINNER, BigDecimal.ZERO);

        Course c2 = buildCourse(202L, "React UI", "Frontend dev", "Frontend",
                CourseLevel.ADVANCED, BigDecimal.TEN);

        Course c3 = buildCourse(203L, "Java Microservices", "Backend", "Backend",
                CourseLevel.BEGINNER, BigDecimal.ZERO);

        when(courseRepository.findAll())
                .thenReturn(List.of(c1, c2, c3));

        LearningPathResponseDto result = service.getLearningPath(1L, "backend");

        assertNotNull(result);
        assertEquals("backend", result.getGoalTitle());
        assertEquals(2, result.getRecommendedCourseIds().size());
    }

    @Test
    void getLearningPath_shouldSkipEnrolledCourses() {

        when(enrollmentRepository.findByStudentId(1L))
                .thenReturn(List.of(enrollment1));

        Course enrolledCourse = buildCourse(101L, "Java Basics", "", "Programming",
                CourseLevel.BEGINNER, BigDecimal.ZERO);

        Course newCourse = buildCourse(201L, "Spring Boot", "Backend", "Backend",
                CourseLevel.BEGINNER, BigDecimal.ZERO);

        when(courseRepository.findAll())
                .thenReturn(List.of(enrolledCourse, newCourse));

        LearningPathResponseDto result = service.getLearningPath(1L, "backend");

        assertEquals(1, result.getRecommendedCourseIds().size());
        assertEquals(201L, result.getRecommendedCourseIds().get(0));
    }

    @Test
    void getLearningPath_shouldReturnEmpty_whenNoMatchingCourses() {

        when(enrollmentRepository.findByStudentId(1L))
                .thenReturn(List.of());

        Course c1 = buildCourse(201L, "Cooking", "Food", "Lifestyle",
                CourseLevel.ADVANCED, BigDecimal.TEN);

        when(courseRepository.findAll()).thenReturn(List.of(c1));

        LearningPathResponseDto result = service.getLearningPath(1L, "backend");

        assertEquals(0, result.getRecommendedCourseIds().size());
    }

    @Test
    void getLearningPath_shouldSetDefaultGoal_whenGoalIsNull() {

        when(enrollmentRepository.findByStudentId(1L)).thenReturn(List.of());
        when(courseRepository.findAll()).thenReturn(List.of());

        LearningPathResponseDto result = service.getLearningPath(1L, null);

        assertEquals("General Learning", result.getGoalTitle());
    }

    // ===========================
    // Helper
    // ===========================

    private Course buildCourse(Long id, String title, String desc, String category,
                               CourseLevel level, BigDecimal price) {
        return Course.builder()
                .id(id)
                .title(title)
                .description(desc)
                .category(category)
                .level(level)
                .price(price)
                .status(CourseStatus.PUBLISHED)
                .build();
    }
}