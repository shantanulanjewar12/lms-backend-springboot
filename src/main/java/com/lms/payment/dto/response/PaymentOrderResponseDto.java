package com.lms.payment.dto.response;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentOrderResponseDto {
	private Long orderId;
	private String stripeSessionId;
	private BigDecimal amount;
	private String currency;
	private String status;
	private String stripePublishableKey;
	private String checkoutUrl;
}