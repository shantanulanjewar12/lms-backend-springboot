package com.lms.enrollment.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class ProgressResponseDto {
	private Long enrollmentId;
	private Long courseId;
	private Double completionPercentage;
	private List<LessonProgressItemDto> lessonProgressList;

	@Data
	public static class LessonProgressItemDto {
		private Long lessonId;
		private Boolean isCompleted;
		private Integer watchDurationSeconds;
	}
}