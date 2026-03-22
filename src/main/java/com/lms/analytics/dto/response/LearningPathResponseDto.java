package com.lms.analytics.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class LearningPathResponseDto {
	private String goalTitle;
	private List<Long> recommendedCourseIds;
	private String description;
}