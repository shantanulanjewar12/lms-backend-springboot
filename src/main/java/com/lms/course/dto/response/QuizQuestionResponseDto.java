package com.lms.course.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class QuizQuestionResponseDto {
	private Long id;
	private Long lessonId;
	private String questionText;
	private String optionA;
	private String optionB;
	private String optionC;
	private String optionD;
	private List<String> correctOptions;
	private Integer orderIndex;
}
