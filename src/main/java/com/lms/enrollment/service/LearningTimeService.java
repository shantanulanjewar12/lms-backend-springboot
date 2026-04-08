package com.lms.enrollment.service;

import com.lms.enrollment.entity.Enrollment;

public interface LearningTimeService {
	
	/**
	 * Update the total learning time for an enrollment by adding the given duration
	 * @param enrollment The enrollment to update
	 * @param durationSeconds Duration to add in seconds
	 */
	void addLearningTime(Enrollment enrollment, long durationSeconds);
	
	/**
	 * Get total learning time formatted as "Xh Ym"
	 * @param totalSeconds Total seconds
	 * @return Formatted string like "3h 25m"
	 */
	String formatLearningTime(Long totalSeconds);
	
	/**
	 * Get total learning time in seconds for an enrollment
	 * @param enrollmentId The enrollment ID
	 * @return Total time in seconds
	 */
	Long getTotalLearningTime(Long enrollmentId);
	
	/**
	 * Update enrollment's lastAccessedAt timestamp
	 * @param enrollment The enrollment to update
	 */
	void updateLastAccessedTime(Enrollment enrollment);
}
