package com.lms.payment.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentRequestDto {
	@NotNull
	private Long courseId;
	@NotNull
	private BigDecimal amount;
	private String currency = "INR";
}