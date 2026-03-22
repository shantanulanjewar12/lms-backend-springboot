package com.lms.enrollment.dto.request;

import java.math.BigDecimal;

import com.lms.enrollment.vo.LearningEventType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearningEventRequestDto {
	@NotNull
	private Long lessonId;
	@NotNull
	private LearningEventType eventType;
	private BigDecimal value;
}