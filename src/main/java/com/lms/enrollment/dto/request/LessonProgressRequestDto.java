package com.lms.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonProgressRequestDto {
	@NotNull
	private Long lessonId;
	private Boolean isCompleted = false;
	private Integer watchDurationSeconds = 0;
}