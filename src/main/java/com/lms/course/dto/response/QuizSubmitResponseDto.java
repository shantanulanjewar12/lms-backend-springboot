package com.lms.course.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class QuizSubmitResponseDto {
	private Long lessonId;
	private Integer totalQuestions;
	private Integer attemptedQuestions;
	private Integer correctAnswers;
	private Double scorePercent;
	private Boolean passed;
	private List<AnswerResultItemDto> answerResults;

	@Data
	public static class AnswerResultItemDto {
		private Long questionId;
		private List<String> selectedOptions;
		private List<String> correctOptions;
		private Boolean isCorrect;
	}
}
