package com.lms.course.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizQuestionUpsertRequestDto {

	@NotBlank
	private String questionText;

	@NotBlank
	private String optionA;

	@NotBlank
	private String optionB;

	@NotBlank
	private String optionC;

	@NotBlank
	private String optionD;

	@NotEmpty
	private List<String> correctOptions;

	@NotNull
	private Integer orderIndex;
}
