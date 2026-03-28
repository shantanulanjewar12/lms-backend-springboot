package com.lms.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lms.payment.vo.PaymentStatus;

import lombok.Data;

@Data
public class PaymentHistoryResponseDto {
	private Long id;
	private Long studentId;
	private String studentName;
	private Long courseId;
	private String courseTitle;
	private BigDecimal amount;
	private String currency;
	private PaymentStatus status;
	private String stripeSessionId;
	private LocalDateTime createdAt;
}