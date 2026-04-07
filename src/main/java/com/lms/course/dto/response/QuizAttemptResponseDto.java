package com.lms.course.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class QuizAttemptResponseDto {
	private Long id;
	private Long studentId;
	private Long courseId;
	private Long quizId;
	private Map<Long, List<String>> answers;
	private Double scorePercent;
	private LocalDateTime submittedAt;
}
