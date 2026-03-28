package com.lms.analytics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.analytics.dto.response.CourseEngagementResponseDto;
import com.lms.analytics.dto.response.InstructorInsightResponseDto;
import com.lms.analytics.dto.response.LessonDifficultyResponseDto;
import com.lms.analytics.service.LessonDifficultyService;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ReviewRepository;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.shared.dto.ApiResponse;
import com.lms.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class InstructorAnalyticsController {

	private final LessonDifficultyService lessonDifficultyService;
	private final CourseRepository courseRepository;
	private final LessonRepository lessonRepository;
	private final ReviewRepository reviewRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final LessonProgressRepository lessonProgressRepository;

	@GetMapping("/instructors/{id}/insights")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<InstructorInsightResponseDto>> getInstructorInsights(@PathVariable Long id,
			Authentication auth) {
		enforceInstructorSelfOrAdmin(auth, id);

		List<Course> instructorCourses = courseRepository.findByInstructorId(id);
		long totalCourses = instructorCourses.size();
		long totalStudents = instructorCourses.stream().mapToLong(c -> enrollmentRepository.countByCourseId(c.getId()))
				.sum();
		double averageRating = calculateInstructorAverageRating(instructorCourses);
		double avgCompletionRate = calculateInstructorAverageCompletion(instructorCourses);

		InstructorInsightResponseDto dto = new InstructorInsightResponseDto();
		dto.setInstructorId(id);
		dto.setTotalCourses(totalCourses);
		dto.setTotalStudents(totalStudents);
		dto.setAverageRating(averageRating);
		dto.setAvgCompletionRate(avgCompletionRate);
		dto.setLessonDifficultyList(List.of());
		return ResponseEntity.ok(ApiResponse.success("Instructor insights", dto));
	}

	@GetMapping("/courses/{id}/difficulty")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<List<LessonDifficultyResponseDto>>> getDifficulty(@PathVariable Long id,
			Authentication auth) {
		enforceCourseOwnershipOrAdmin(auth, id);
		return ResponseEntity
				.ok(ApiResponse.success("Lesson difficulty", lessonDifficultyService.getDifficultyForCourse(id)));
	}

	@GetMapping("/courses/{id}/engagement")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<CourseEngagementResponseDto>> getEngagement(@PathVariable Long id,
			Authentication auth) {
		enforceCourseOwnershipOrAdmin(auth, id);
		long totalEnrollments = enrollmentRepository.countByCourseId(id);
		CourseEngagementResponseDto dto = new CourseEngagementResponseDto();
		dto.setCourseId(id);
		dto.setTotalEnrollments(totalEnrollments);
		dto.setCompletionRate(0.0);
		dto.setAverageWatchTimeSeconds(0.0);
		return ResponseEntity.ok(ApiResponse.success("Course engagement", dto));
	}

	private void enforceInstructorSelfOrAdmin(Authentication auth, Long instructorId) {
		if (isAdmin(auth)) {
			return;
		}
		Long currentUserId = (Long) auth.getCredentials();
		if (!currentUserId.equals(instructorId)) {
			throw new AccessDeniedException("You can only access your own instructor analytics");
		}
	}

	private void enforceCourseOwnershipOrAdmin(Authentication auth, Long courseId) {
		if (isAdmin(auth)) {
			return;
		}
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
		Long currentUserId = (Long) auth.getCredentials();
		if (!currentUserId.equals(course.getInstructorId())) {
			throw new AccessDeniedException("You can only access analytics for your own courses");
		}
	}

	private boolean isAdmin(Authentication auth) {
		return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
	}

	private double calculateInstructorAverageRating(List<Course> courses) {
		double weightedSum = 0.0;
		long totalReviews = 0;

		for (Course course : courses) {
			long reviewCount = reviewRepository.countByCourseId(course.getId());
			if (reviewCount == 0) {
				continue;
			}
			Double avgRating = reviewRepository.findAverageRatingByCourseId(course.getId());
			if (avgRating == null) {
				continue;
			}
			weightedSum += avgRating * reviewCount;
			totalReviews += reviewCount;
		}

		return totalReviews > 0 ? weightedSum / totalReviews : 0.0;
	}

	private double calculateInstructorAverageCompletion(List<Course> courses) {
		double completionPercentSum = 0.0;
		long enrollmentCount = 0;

		for (Course course : courses) {
			long totalLessons = lessonRepository.countByCourseId(course.getId());
			List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());

			for (Enrollment enrollment : enrollments) {
				double completionPercent = 0.0;
				if (totalLessons > 0) {
					long completedLessons = lessonProgressRepository
							.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());
					completionPercent = (completedLessons * 100.0) / totalLessons;
				}
				completionPercentSum += completionPercent;
				enrollmentCount++;
			}
		}

		return enrollmentCount > 0 ? completionPercentSum / enrollmentCount : 0.0;
	}
}