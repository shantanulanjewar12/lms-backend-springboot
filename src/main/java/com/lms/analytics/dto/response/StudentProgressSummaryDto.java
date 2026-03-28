package com.lms.analytics.dto.response;

import lombok.Data;

@Data
public class StudentProgressSummaryDto {
	private Long studentId;
	private int totalEnrolled;
	private int totalCompleted;
	private Double averageCompletionPercent;
	private int streakDays;
}