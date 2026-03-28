package com.lms.enrollment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollRequestDto {
	@NotNull
	private Long courseId;
	private String paymentReferenceId;
}