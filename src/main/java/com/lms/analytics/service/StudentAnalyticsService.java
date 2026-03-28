package com.lms.analytics.service;

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;

public interface StudentAnalyticsService {
	StudentProgressSummaryDto getStudentSummary(Long studentId);

	LearningPathResponseDto getLearningPath(Long studentId, String goal);
}