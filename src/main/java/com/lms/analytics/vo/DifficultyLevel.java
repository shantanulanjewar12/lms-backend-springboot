package com.lms.analytics.vo;

public enum DifficultyLevel {
	EASY, MEDIUM, DIFFICULT;

	public String description() {
		return switch (this) {
		case EASY -> "Easy";
		case MEDIUM -> "Medium";
		case DIFFICULT -> "Difficult — consider revising this lesson";
		};
	}
}