package com.lms.analytics.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;
import com.lms.analytics.service.StudentAnalyticsService;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;
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
	private final CourseRepository courseRepository;

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
	public LearningPathResponseDto getLearningPath(Long studentId, String goal) {
		String normalizedGoal = goal == null ? "" : goal.trim();
		List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
		Set<Long> enrolledCourseIds = new HashSet<>(enrollments.stream().map(Enrollment::getCourseId).toList());
		long completedCount = enrollments.stream().filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED).count();

		CourseLevel preferredLevel = preferredLevelForStudent(completedCount);
		Set<String> goalKeywords = keywordsForGoal(normalizedGoal);

		List<CourseScore> scored = new ArrayList<>();
		for (Course course : courseRepository.findAll()) {
			if (course.getStatus() != CourseStatus.PUBLISHED) {
				continue;
			}
			if (enrolledCourseIds.contains(course.getId())) {
				continue;
			}

			double score = scoreCourse(course, goalKeywords, preferredLevel);
			if (score > 0) {
				scored.add(new CourseScore(course.getId(), score));
			}
		}

		scored.sort(Comparator.comparingDouble(CourseScore::score).reversed().thenComparingLong(CourseScore::courseId));
		List<Long> recommendations = scored.stream().limit(6).map(CourseScore::courseId).toList();

		LearningPathResponseDto dto = new LearningPathResponseDto();
		dto.setGoalTitle(normalizedGoal.isBlank() ? "General Learning" : normalizedGoal);
		dto.setDescription(buildDescription(dto.getGoalTitle(), recommendations.size(), preferredLevel));
		dto.setRecommendedCourseIds(recommendations);
		return dto;
	}

	private Set<String> keywordsForGoal(String goal) {
		String g = goal == null ? "" : goal.toLowerCase(Locale.ROOT);
		if (g.contains("backend"))
			return Set.of("backend", "java", "spring", "api", "database", "microservice");
		if (g.contains("frontend") || g.contains("ui") || g.contains("ux"))
			return Set.of("frontend", "react", "javascript", "css", "html", "ui", "ux");
		if (g.contains("data") || g.contains("ml") || g.contains("ai"))
			return Set.of("data", "python", "machine learning", "ml", "ai", "analytics");
		if (g.contains("devops"))
			return Set.of("devops", "docker", "kubernetes", "cloud", "ci", "cd");
		if (g.contains("mobile"))
			return Set.of("mobile", "android", "ios", "flutter", "react native");
		return Set.of("programming", "software", "development", "coding");
	}

	private double scoreCourse(Course course, Set<String> keywords, CourseLevel preferredLevel) {
		double score = 0.0;
		String haystack = ((course.getTitle() == null ? "" : course.getTitle()) + " "
				+ (course.getDescription() == null ? "" : course.getDescription()) + " "
				+ (course.getCategory() == null ? "" : course.getCategory())).toLowerCase(Locale.ROOT);

		for (String keyword : keywords) {
			if (haystack.contains(keyword)) {
				score += 25.0;
			}
		}

		if (course.getLevel() == preferredLevel) {
			score += 20.0;
		} else if (preferredLevel == CourseLevel.INTERMEDIATE && course.getLevel() == CourseLevel.BEGINNER) {
			score += 10.0;
		} else if (preferredLevel == CourseLevel.ADVANCED && course.getLevel() == CourseLevel.INTERMEDIATE) {
			score += 10.0;
		}

		if (course.getPrice() != null && course.getPrice().signum() == 0) {
			score += 5.0;
		}

		return score;
	}

	private CourseLevel preferredLevelForStudent(long completedCount) {
		if (completedCount <= 1) {
			return CourseLevel.BEGINNER;
		}
		if (completedCount <= 4) {
			return CourseLevel.INTERMEDIATE;
		}
		return CourseLevel.ADVANCED;
	}

	private String buildDescription(String goal, int recommendationCount, CourseLevel preferredLevel) {
		if (recommendationCount == 0) {
			return "No matching courses found yet for " + goal
					+ ". Publish or enroll in more courses and try again.";
		}
		return "Recommended roadmap for " + goal + " with " + recommendationCount
				+ " curated courses at " + preferredLevel.name().toLowerCase(Locale.ROOT) + " level focus.";
	}

	private record CourseScore(Long courseId, double score) {
	}
}