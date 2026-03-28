package com.lms.analytics.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class InstructorInsightResponseDto {
	private Long instructorId;
	private long totalCourses;
	private Double averageRating;
	private long totalStudents;
	private Double avgCompletionRate;
	private List<LessonDifficultyResponseDto> lessonDifficultyList;
}