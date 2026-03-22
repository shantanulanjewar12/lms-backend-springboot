package com.lms.course.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class QuizSubmitRequestDto {

	@NotEmpty
	@Valid
	private List<QuizAnswerItemRequestDto> answers;
}
