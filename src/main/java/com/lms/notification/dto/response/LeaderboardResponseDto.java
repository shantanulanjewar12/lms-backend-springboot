package com.lms.notification.dto.response;

import lombok.Data;

@Data
public class LeaderboardResponseDto {
	private int rank;
	private Long studentId;
	private String studentName;
	private int totalCoursesCompleted;
	private int currentStreak;
	private long totalBadges;
}