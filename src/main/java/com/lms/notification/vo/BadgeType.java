package com.lms.notification.vo;

public enum BadgeType {
	FIRST_LESSON, COURSE_COMPLETE, STREAK_7, TOP_LEARNER;

	public String badgeLabel() {
		return switch (this) {
		case FIRST_LESSON -> "First Step";
		case COURSE_COMPLETE -> "Course Champion";
		case STREAK_7 -> "7-Day Streak";
		case TOP_LEARNER -> "Top Learner";
		};
	}

	public String description() {
		return switch (this) {
		case FIRST_LESSON -> "Completed your first lesson!";
		case COURSE_COMPLETE -> "Completed an entire course!";
		case STREAK_7 -> "Learned 7 days in a row!";
		case TOP_LEARNER -> "Ranked in the top learners!";
		};
	}
}