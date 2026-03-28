package com.lms.notification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BadgeSchedulerService {

	private final BadgeService badgeService;

	/**
	 * Scheduled task to check and award STREAK_7 badges.
	 * Runs daily at 2 AM.
	 */
	@Scheduled(cron = "0 0 2 * * *")
	public void checkStreakBadges() {
		badgeService.checkAndAwardStreakBadges();
	}

	/**
	 * Scheduled task to check and award TOP_LEARNER badges.
	 * Runs daily at 3 AM.
	 */
	@Scheduled(cron = "0 0 3 * * *")
	public void checkTopLearnerBadges() {
		badgeService.checkAndAwardTopLearnerBadges();
	}
}
