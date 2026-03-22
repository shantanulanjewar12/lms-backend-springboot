package com.lms.analytics.vo;

public record DifficultyScoreVO(double score) {
	public DifficultyLevel label() {
		if (score > 0.6)
			return DifficultyLevel.DIFFICULT;
		if (score >= 0.3)
			return DifficultyLevel.MEDIUM;
		return DifficultyLevel.EASY;
	}
}