package com.lms.course.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QuizAnswerItemRequestDto {

	@NotNull
	private Long questionId;

	@NotEmpty
	private List<String> selectedOptions;
}
