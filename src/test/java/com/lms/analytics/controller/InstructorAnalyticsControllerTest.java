package com.lms.analytics.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lms.analytics.service.LessonDifficultyService;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ReviewRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;

@ExtendWith(MockitoExtension.class)
class InstructorAnalyticsControllerTest {

    @Mock
    private LessonDifficultyService lessonDifficultyService;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @InjectMocks
    private InstructorAnalyticsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getInstructorInsights_shouldReturnSuccess() throws Exception {
        Course course = new Course();
        course.setId(101L);
        course.setInstructorId(1L);

        Enrollment enrollment = Enrollment.builder()
                .id(11L)
                .courseId(101L)
                .studentId(10L)
                .build();

        when(courseRepository.findByInstructorId(1L)).thenReturn(List.of(course));
        when(enrollmentRepository.countByCourseId(101L)).thenReturn(5L);
        when(reviewRepository.countByCourseId(101L)).thenReturn(2L);
        when(reviewRepository.findAverageRatingByCourseId(101L)).thenReturn(4.5);
        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(enrollmentRepository.findByCourseId(101L)).thenReturn(List.of(enrollment));
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(11L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/analytics/instructors/1/insights")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.instructorId").value(1))
                .andExpect(jsonPath("$.data.totalCourses").value(1))
                .andExpect(jsonPath("$.data.totalStudents").value(5))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.avgCompletionRate").value(50.0));
    }

    @Test
    void getInstructorInsights_shouldThrowAccessDenied() {
        assertThrows(AccessDeniedException.class,
                () -> controller.getInstructorInsights(1L, createAuth(2L, "ROLE_INSTRUCTOR")));
    }

    @Test
    void getDifficulty_shouldReturnSuccess() throws Exception {
        Course course = new Course();
        course.setId(101L);
        course.setInstructorId(1L);

        when(courseRepository.findById(101L)).thenReturn(Optional.of(course));
        when(lessonDifficultyService.getDifficultyForCourse(101L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/analytics/courses/101/difficulty")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getEngagement_shouldReturnSuccess() throws Exception {
        Course course = new Course();
        course.setId(101L);
        course.setInstructorId(1L);

        when(courseRepository.findById(101L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.countByCourseId(101L)).thenReturn(10L);

        mockMvc.perform(get("/api/v1/analytics/courses/101/engagement")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.totalEnrollments").value(10))
                .andExpect(jsonPath("$.data.completionRate").value(0.0))
                .andExpect(jsonPath("$.data.averageWatchTimeSeconds").value(0.0));
    }

    private Authentication createAuth(Long userId, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return userId;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "user";
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return "user";
            }
        };
    }
}