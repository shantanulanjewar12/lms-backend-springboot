package com.lms.notification.service;

import java.util.List;

import com.lms.notification.dto.response.BadgeResponseDto;
import com.lms.notification.vo.BadgeType;

public interface BadgeService {
	void awardBadge(Long studentId, BadgeType badgeType);

	List<BadgeResponseDto> getStudentBadges(Long studentId);
	
	void checkAndAwardStreakBadges();
	
	void checkAndAwardTopLearnerBadges();
}