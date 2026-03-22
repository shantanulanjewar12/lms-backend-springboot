package com.lms.analytics.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;
import com.lms.analytics.service.StudentAnalyticsService;
import com.lms.course.repository.LessonRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.repository.LearningStreakRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentAnalyticsServiceImpl implements StudentAnalyticsService {

	private final EnrollmentRepository enrollmentRepository;
	private final LessonProgressRepository lessonProgressRepository;
	private final LessonRepository lessonRepository;
	private final LearningStreakRepository learningStreakRepository;

	@Override
	public StudentProgressSummaryDto getStudentSummary(Long studentId) {
		List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
		int totalEnrolled = enrollments.size();
		int totalCompleted = (int) enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
				.count();

		double avgCompletion = enrollments.stream().mapToDouble(e -> {
			long total = lessonRepository.countByCourseId(e.getCourseId());
			long done = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(e.getId());
			return total > 0 ? (done * 100.0 / total) : 0.0;
		}).average().orElse(0.0);

		int streak = learningStreakRepository.findByStudentId(studentId).map(s -> s.getCurrentStreak()).orElse(0);

		StudentProgressSummaryDto dto = new StudentProgressSummaryDto();
		dto.setStudentId(studentId);
		dto.setTotalEnrolled(totalEnrolled);
		dto.setTotalCompleted(totalCompleted);
		dto.setAverageCompletionPercent(avgCompletion);
		dto.setStreakDays(streak);
		return dto;
	}

	@Override
	public LearningPathResponseDto getLearningPath(String goal) {
		// Simple rule-based recommendation — can be enhanced with ML
		LearningPathResponseDto dto = new LearningPathResponseDto();
		dto.setGoalTitle(goal);
		dto.setDescription("Recommended learning path for: " + goal);
		dto.setRecommendedCourseIds(List.of()); // Populate with real recommendation logic
		return dto;
	}
}