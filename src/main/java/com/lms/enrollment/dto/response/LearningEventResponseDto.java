package com.lms.enrollment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lms.enrollment.vo.LearningEventType;

import lombok.Data;

@Data
public class LearningEventResponseDto {
	private Long id;
	private Long studentId;
	private Long lessonId;
	private LearningEventType eventType;
	private BigDecimal value;
	private LocalDateTime createdAt;
}