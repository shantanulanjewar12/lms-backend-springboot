package com.lms.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.notification.dto.response.BadgeResponseDto;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.entity.StudentBadge;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.vo.BadgeType;

@ExtendWith(MockitoExtension.class)
class BadgeServiceImplTest {

    @Mock
    private StudentBadgeRepository badgeRepository;

    @Mock
    private LearningStreakRepository streakRepository;

    @InjectMocks
    private BadgeServiceImpl badgeService;

    private StudentBadge studentBadge;
    private LearningStreak streak1;
    private LearningStreak streak2;

    @BeforeEach
    void setUp() {
        studentBadge = StudentBadge.builder()
                .id(1L)
                .studentId(10L)
                .badgeType(BadgeType.FIRST_LESSON)
                .awardedAt(LocalDateTime.now())
                .build();

        streak1 = LearningStreak.builder()
                .studentId(10L)
                .currentStreak(7)
                .longestStreak(7)
                .build();

        streak2 = LearningStreak.builder()
                .studentId(20L)
                .currentStreak(3)
                .longestStreak(5)
                .build();
    }

    @Test
    void awardBadge_shouldSaveBadge() {
        badgeService.awardBadge(10L, BadgeType.FIRST_LESSON);

        ArgumentCaptor<StudentBadge> captor = ArgumentCaptor.forClass(StudentBadge.class);
        verify(badgeRepository).save(captor.capture());

        StudentBadge savedBadge = captor.getValue();
        assertEquals(10L, savedBadge.getStudentId());
        assertEquals(BadgeType.FIRST_LESSON, savedBadge.getBadgeType());
    }

    @Test
    void getStudentBadges_shouldReturnMappedBadgeDtos() {
        when(badgeRepository.findByStudentId(10L)).thenReturn(List.of(studentBadge));

        List<BadgeResponseDto> result = badgeService.getStudentBadges(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getStudentId());
        assertEquals(BadgeType.FIRST_LESSON, result.get(0).getBadgeType());
        assertEquals(BadgeType.FIRST_LESSON.badgeLabel(), result.get(0).getBadgeLabel());
        assertEquals(BadgeType.FIRST_LESSON.description(), result.get(0).getBadgeDescription());
        assertEquals(studentBadge.getAwardedAt(), result.get(0).getAwardedAt());
    }

    @Test
    void getStudentBadges_shouldReturnEmptyList_whenNoBadges() {
        when(badgeRepository.findByStudentId(10L)).thenReturn(List.of());

        List<BadgeResponseDto> result = badgeService.getStudentBadges(10L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void checkAndAwardStreakBadges_shouldAwardBadge_whenStreakIsAtLeast7AndNotAlreadyAwarded() {
        when(streakRepository.findAll()).thenReturn(List.of(streak1, streak2));
        when(badgeRepository.existsByStudentIdAndBadgeType(10L, BadgeType.STREAK_7)).thenReturn(false);

        badgeService.checkAndAwardStreakBadges();

        verify(badgeRepository).save(any(StudentBadge.class));
    }

    @Test
    void checkAndAwardStreakBadges_shouldNotAward_whenBadgeAlreadyExists() {
        when(streakRepository.findAll()).thenReturn(List.of(streak1));
        when(badgeRepository.existsByStudentIdAndBadgeType(10L, BadgeType.STREAK_7)).thenReturn(true);

        badgeService.checkAndAwardStreakBadges();

        verify(badgeRepository, never()).save(any(StudentBadge.class));
    }

    @Test
    void checkAndAwardStreakBadges_shouldNotAward_whenStreakLessThan7() {
        when(streakRepository.findAll()).thenReturn(List.of(streak2));

        badgeService.checkAndAwardStreakBadges();

        verify(badgeRepository, never()).save(any(StudentBadge.class));
    }

    @Test
    void checkAndAwardTopLearnerBadges_shouldAwardTopLearnerBadge_toTopStudentsWithoutBadge() {
        LearningStreak s1 = LearningStreak.builder().studentId(1L).currentStreak(5).build();
        LearningStreak s2 = LearningStreak.builder().studentId(2L).currentStreak(6).build();
        LearningStreak s3 = LearningStreak.builder().studentId(3L).currentStreak(7).build();
        LearningStreak s4 = LearningStreak.builder().studentId(4L).currentStreak(8).build();
        LearningStreak s5 = LearningStreak.builder().studentId(5L).currentStreak(9).build();
        LearningStreak s6 = LearningStreak.builder().studentId(6L).currentStreak(10).build();

        when(streakRepository.findAll()).thenReturn(List.of(s1, s2, s3, s4, s5, s6));

        when(badgeRepository.countByStudentId(1L)).thenReturn(1L);
        when(badgeRepository.countByStudentId(2L)).thenReturn(6L);
        when(badgeRepository.countByStudentId(3L)).thenReturn(4L);
        when(badgeRepository.countByStudentId(4L)).thenReturn(3L);
        when(badgeRepository.countByStudentId(5L)).thenReturn(2L);
        when(badgeRepository.countByStudentId(6L)).thenReturn(5L);

        when(badgeRepository.existsByStudentIdAndBadgeType(2L, BadgeType.TOP_LEARNER)).thenReturn(false);
        when(badgeRepository.existsByStudentIdAndBadgeType(6L, BadgeType.TOP_LEARNER)).thenReturn(false);
        when(badgeRepository.existsByStudentIdAndBadgeType(3L, BadgeType.TOP_LEARNER)).thenReturn(false);
        when(badgeRepository.existsByStudentIdAndBadgeType(4L, BadgeType.TOP_LEARNER)).thenReturn(false);
        when(badgeRepository.existsByStudentIdAndBadgeType(5L, BadgeType.TOP_LEARNER)).thenReturn(false);

        badgeService.checkAndAwardTopLearnerBadges();

        verify(badgeRepository, times(5)).save(any(StudentBadge.class));
    }

    @Test
    void checkAndAwardTopLearnerBadges_shouldSkipStudentsWhoAlreadyHaveTopLearnerBadge() {
        LearningStreak s1 = LearningStreak.builder().studentId(1L).currentStreak(5).build();
        LearningStreak s2 = LearningStreak.builder().studentId(2L).currentStreak(6).build();
        LearningStreak s3 = LearningStreak.builder().studentId(3L).currentStreak(7).build();
        LearningStreak s4 = LearningStreak.builder().studentId(4L).currentStreak(8).build();
        LearningStreak s5 = LearningStreak.builder().studentId(5L).currentStreak(9).build();

        when(streakRepository.findAll()).thenReturn(List.of(s1, s2, s3, s4, s5));

        when(badgeRepository.countByStudentId(1L)).thenReturn(5L);
        when(badgeRepository.countByStudentId(2L)).thenReturn(4L);
        when(badgeRepository.countByStudentId(3L)).thenReturn(3L);
        when(badgeRepository.countByStudentId(4L)).thenReturn(2L);
        when(badgeRepository.countByStudentId(5L)).thenReturn(1L);

        when(badgeRepository.existsByStudentIdAndBadgeType(1L, BadgeType.TOP_LEARNER)).thenReturn(true);
        when(badgeRepository.existsByStudentIdAndBadgeType(2L, BadgeType.TOP_LEARNER)).thenReturn(true);
        when(badgeRepository.existsByStudentIdAndBadgeType(3L, BadgeType.TOP_LEARNER)).thenReturn(true);
        when(badgeRepository.existsByStudentIdAndBadgeType(4L, BadgeType.TOP_LEARNER)).thenReturn(true);
        when(badgeRepository.existsByStudentIdAndBadgeType(5L, BadgeType.TOP_LEARNER)).thenReturn(true);

        badgeService.checkAndAwardTopLearnerBadges();

        verify(badgeRepository, never()).save(any(StudentBadge.class));
    }
}