package com.lms.notification.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lms.notification.dto.response.BadgeResponseDto;
import com.lms.notification.entity.StudentBadge;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.service.BadgeService;
import com.lms.notification.vo.BadgeType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements BadgeService {

	private final StudentBadgeRepository badgeRepository;
	private final LearningStreakRepository streakRepository;

	@Override
	public void awardBadge(Long studentId, BadgeType badgeType) {
		StudentBadge badge = StudentBadge.builder().studentId(studentId).badgeType(badgeType).build();
		badgeRepository.save(badge);
	}

	@Override
	public List<BadgeResponseDto> getStudentBadges(Long studentId) {
		return badgeRepository.findByStudentId(studentId).stream().map(b -> {
			BadgeResponseDto dto = new BadgeResponseDto();
			dto.setId(b.getId());
			dto.setStudentId(b.getStudentId());
			dto.setBadgeType(b.getBadgeType());
			dto.setBadgeLabel(b.getBadgeType().badgeLabel());
			dto.setBadgeDescription(b.getBadgeType().description());
			dto.setAwardedAt(b.getAwardedAt());
			return dto;
		}).toList();
	}
	
	@Override
	public void checkAndAwardStreakBadges() {
		// Get all learning streaks
		List<LearningStreak> streaks = streakRepository.findAll();
		
		for (LearningStreak streak : streaks) {
			if (streak.getCurrentStreak() >= 7 && streak.getStudentId() != null) {
				// Check if student doesn't already have STREAK_7 badge
				if (!badgeRepository.existsByStudentIdAndBadgeType(streak.getStudentId(), BadgeType.STREAK_7)) {
					awardBadge(streak.getStudentId(), BadgeType.STREAK_7);
				}
			}
		}
	}
	
	@Override
	public void checkAndAwardTopLearnerBadges() {
		// Get all students' badge counts and sort by count descending
		List<LearningStreak> streaks = streakRepository.findAll();
		
		// Determine top threshold (e.g., top 10% or top 5 students)
		int topCount = Math.max(1, Math.max(5, streaks.size() / 10)); // Top 10% or at least 5, minimum 1
		
		streaks.stream()
			.sorted((s1, s2) -> {
				long count1 = badgeRepository.countByStudentId(s1.getStudentId());
				long count2 = badgeRepository.countByStudentId(s2.getStudentId());
				return Long.compare(count2, count1); // Descending by badge count
			})
			.limit(topCount)
			.forEach(streak -> {
				if (!badgeRepository.existsByStudentIdAndBadgeType(streak.getStudentId(), BadgeType.TOP_LEARNER)) {
					awardBadge(streak.getStudentId(), BadgeType.TOP_LEARNER);
				}
			});
	}
}