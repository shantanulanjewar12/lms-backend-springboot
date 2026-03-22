package com.lms.analytics.dto.response;

import lombok.Data;

@Data
public class CourseEngagementResponseDto {
	private Long courseId;
	private Double completionRate;
	private Double averageWatchTimeSeconds;
	private long totalEnrollments;
	private Long dropOffLessonId;
}