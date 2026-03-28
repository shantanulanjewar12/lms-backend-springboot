package com.lms.notification.controller;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lms.notification.dto.response.BadgeResponseDto;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.service.BadgeService;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EngagementControllerTest {

    @Mock
    private BadgeService badgeService;

    @Mock
    private LearningStreakRepository streakRepository;

    @Mock
    private StudentBadgeRepository badgeRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EngagementController engagementController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(engagementController).build();
    }

    @Test
    void getBadges_shouldReturnBadges() throws Exception {
        BadgeResponseDto badge = new BadgeResponseDto();
        badge.setId(1L);
        badge.setStudentId(10L);
        badge.setBadgeLabel("First Lesson");
        badge.setBadgeDescription("Completed first lesson");

        when(badgeService.getStudentBadges(10L)).thenReturn(List.of(badge));

        mockMvc.perform(get("/api/v1/engagement/badges/10")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Badges fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].studentId").value(10))
                .andExpect(jsonPath("$.data[0].badgeLabel").value("First Lesson"));
    }

    @Test
    void getStreak_shouldReturnExistingStreak() throws Exception {
        LearningStreak streak = LearningStreak.builder()
                .studentId(10L)
                .currentStreak(5)
                .longestStreak(7)
                .build();

        when(streakRepository.findByStudentId(10L)).thenReturn(Optional.of(streak));

        mockMvc.perform(get("/api/v1/engagement/streak/10")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Streak data"))
                .andExpect(jsonPath("$.data.studentId").value(10))
                .andExpect(jsonPath("$.data.currentStreak").value(5))
                .andExpect(jsonPath("$.data.longestStreak").value(7));
    }

    @Test
    void getStreak_shouldReturnDefaultStreakWhenNotFound() throws Exception {
        when(streakRepository.findByStudentId(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/engagement/streak/10")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Streak data"))
                .andExpect(jsonPath("$.data.studentId").value(10));
    }

    @Test
    void getLeaderboard_shouldReturnSortedLeaderboard() throws Exception {
        LearningStreak s1 = LearningStreak.builder()
                .studentId(10L)
                .currentStreak(7)
                .longestStreak(10)
                .build();

        LearningStreak s2 = LearningStreak.builder()
                .studentId(20L)
                .currentStreak(5)
                .longestStreak(8)
                .build();

        User u1 = new User();
        u1.setId(10L);
        u1.setFullName("Priya");

        User u2 = new User();
        u2.setId(20L);
        u2.setFullName("Asha");

        when(streakRepository.findAll()).thenReturn(List.of(s1, s2));

        when(enrollmentRepository.countByStudentIdAndStatus(10L, EnrollmentStatus.COMPLETED)).thenReturn(3L);
        when(enrollmentRepository.countByStudentIdAndStatus(20L, EnrollmentStatus.COMPLETED)).thenReturn(1L);

        when(badgeRepository.countByStudentId(10L)).thenReturn(4L);
        when(badgeRepository.countByStudentId(20L)).thenReturn(2L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(u1));
        when(userRepository.findById(20L)).thenReturn(Optional.of(u2));

        mockMvc.perform(get("/api/v1/engagement/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaderboard"))
                .andExpect(jsonPath("$.data[0].rank").value(1))
                .andExpect(jsonPath("$.data[0].studentId").value(10))
                .andExpect(jsonPath("$.data[0].studentName").value("Priya"))
                .andExpect(jsonPath("$.data[0].currentStreak").value(7))
                .andExpect(jsonPath("$.data[0].totalBadges").value(4))
                .andExpect(jsonPath("$.data[0].totalCoursesCompleted").value(3))
                .andExpect(jsonPath("$.data[1].rank").value(2))
                .andExpect(jsonPath("$.data[1].studentId").value(20))
                .andExpect(jsonPath("$.data[1].studentName").value("Asha"));
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