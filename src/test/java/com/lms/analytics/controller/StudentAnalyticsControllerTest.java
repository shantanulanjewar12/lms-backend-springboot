package com.lms.analytics.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;

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

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;
import com.lms.analytics.service.StudentAnalyticsService;

@ExtendWith(MockitoExtension.class)
class StudentAnalyticsControllerTest {

    @Mock
    private StudentAnalyticsService studentAnalyticsService;

    @InjectMocks
    private StudentAnalyticsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getStudentSummary_shouldReturnSuccess() throws Exception {
        StudentProgressSummaryDto dto = new StudentProgressSummaryDto();
        dto.setStudentId(1L);
        dto.setTotalEnrolled(3);
        dto.setTotalCompleted(1);
        dto.setAverageCompletionPercent(66.5);
        dto.setStreakDays(4);

        when(studentAnalyticsService.getStudentSummary(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/analytics/students/1/summary")
                        .principal(createAuth(1L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.studentId").value(1))
                .andExpect(jsonPath("$.data.totalEnrolled").value(3))
                .andExpect(jsonPath("$.data.totalCompleted").value(1))
                .andExpect(jsonPath("$.data.averageCompletionPercent").value(66.5))
                .andExpect(jsonPath("$.data.streakDays").value(4));
    }

    @Test
    void getLearningPath_shouldReturnSuccess() throws Exception {
        LearningPathResponseDto dto = new LearningPathResponseDto();
        dto.setGoalTitle("backend");
        dto.setDescription("Recommended roadmap for backend");
        dto.setRecommendedCourseIds(List.of(101L, 102L, 103L));

        when(studentAnalyticsService.getLearningPath(1L, "backend")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/analytics/learning-path")
                        .param("goal", "backend")
                        .principal(createAuth(1L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.goalTitle").value("backend"))
                .andExpect(jsonPath("$.data.description").value("Recommended roadmap for backend"))
                .andExpect(jsonPath("$.data.recommendedCourseIds[0]").value(101))
                .andExpect(jsonPath("$.data.recommendedCourseIds[1]").value(102))
                .andExpect(jsonPath("$.data.recommendedCourseIds[2]").value(103));
    }

    @Test
    void getLearningPath_shouldPassStudentIdFromAuthentication() throws Exception {
        LearningPathResponseDto dto = new LearningPathResponseDto();
        dto.setGoalTitle("data science");
        dto.setDescription("Roadmap");
        dto.setRecommendedCourseIds(List.of());

        when(studentAnalyticsService.getLearningPath(5L, "data science")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/analytics/learning-path")
                        .param("goal", "data science")
                        .principal(createAuth(5L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.goalTitle").value("data science"));
    }

    @Test
    void getLearningPath_shouldThrowException_whenAuthenticationIsNull() {
        assertThrows(NullPointerException.class,
                () -> controller.getLearningPath("backend", null));
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
