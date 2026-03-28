package com.lms.course.vo;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PriceVO {
	BigDecimal amount;
	String currency;

	public String formattedDisplay() {
		return currency + " " + amount.toPlainString();
	}
}