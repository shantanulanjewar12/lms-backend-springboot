package com.lms.enrollment.vo;

public record CompletionPercentageVO(double value) {
	public String label() {
		if (value == 0)
			return "Not Started";
		if (value >= 100)
			return "Completed";
		return "In Progress";
	}
}