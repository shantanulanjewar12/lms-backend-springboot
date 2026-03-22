package com.lms.payment.vo;

import java.math.BigDecimal;

public record AmountVO(BigDecimal amount, String currency) {
	public String formattedDisplay() {
		return currency + " " + amount.toPlainString();
	}
}