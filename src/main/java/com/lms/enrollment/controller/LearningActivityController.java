package com.lms.enrollment.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lms.enrollment.dto.response.EnrollmentAccessStatusDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.dto.response.LearningTimeSummaryDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.enrollment.service.LearningTimeService;
import com.lms.enrollment.vo.LearningEventType;
import com.lms.shared.dto.ApiResponse;
import com.lms.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/learning-activity")
@RequiredArgsConstructor
public class LearningActivityController {

	private final EnrollmentService enrollmentService;
	private final LearningTimeService learningTimeService;
	private final LearningEventRepository learningEventRepository;

	/**
	 * Track learning activity - update total time spent
	 * This endpoint should be called when a student:
	 * - Watches a video (duration in seconds)
	 * - Completes a quiz (Quiz attempt time)
	 * - Reads a document (Reading time)
	 */
	@PostMapping("/track/{courseId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<String>> trackLearningActivity(
			Authentication auth,
			@PathVariable Long courseId,
			@RequestParam Long durationSeconds,
			@RequestParam(required = false) Long lessonId,
			@RequestParam(required = false) LearningEventType eventType) {

		Long studentId = (Long) auth.getCredentials();
		if (eventType == null) {
			eventType = LearningEventType.VIDEO_WATCH;
		}
		
		try {
			Enrollment enrollment = enrollmentService.getEnrollmentEntity(studentId, courseId);
			learningTimeService.addLearningTime(enrollment, durationSeconds);
			if (lessonId != null) {
				LearningEvent event = LearningEvent.builder()
					.studentId(studentId)
					.lessonId(lessonId)
					.eventType(eventType)
					.value(BigDecimal.valueOf(durationSeconds))
					.build();
				learningEventRepository.save(event);
			}
			
			return ResponseEntity.ok(ApiResponse.success(
					"Learning activity tracked. Total time: " + 
					learningTimeService.formatLearningTime(enrollment.getTotalTimeSpentSeconds()),
					"Activity tracked successfully"
			));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Enrollment not found for this course"));
		}
	}

	/**
	 * Get enrollment details with learning time
	 */
	@GetMapping("/{courseId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<EnrollmentResponseDto>> getEnrollmentWithLearningTime(
			Authentication auth,
			@PathVariable Long courseId) {

		Long studentId = (Long) auth.getCredentials();
		
		try {
			Enrollment enrollment = enrollmentService.getEnrollmentEntity(studentId, courseId);
			EnrollmentResponseDto dto = toDto(enrollment);
			
			return ResponseEntity.ok(ApiResponse.success(
					"Enrollment details fetched",
					dto
			));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Enrollment not found"));
		}
	}

	/**
	 * Get formatted learning time for an enrollment
	 */
	@GetMapping("/{courseId}/learning-time")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<String>> getLearningTime(
			Authentication auth,
			@PathVariable Long courseId) {

		Long studentId = (Long) auth.getCredentials();
		
		try {
			Enrollment enrollment = enrollmentService.getEnrollmentEntity(studentId, courseId);
			String formattedTime = learningTimeService.formatLearningTime(enrollment.getTotalTimeSpentSeconds());
			
			return ResponseEntity.ok(ApiResponse.success(
					"Your Learning Time: " + formattedTime,
					formattedTime
			));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Enrollment not found"));
		}
	}

	/**
	 * Check if enrollment access has expired
	 */
	@GetMapping("/{courseId}/access-status")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<EnrollmentAccessStatusDto>> checkAccessStatus(
			Authentication auth,
			@PathVariable Long courseId) {

		Long studentId = (Long) auth.getCredentials();
		
		try {
			Enrollment enrollment = enrollmentService.getEnrollmentEntity(studentId, courseId);
			
			boolean hasAccess = enrollment.getExpiresAt() == null || 
					!LocalDateTime.now().isAfter(enrollment.getExpiresAt());
			long remainingDays = enrollment.getExpiresAt() != null ? 
					ChronoUnit.DAYS.between(LocalDateTime.now(), enrollment.getExpiresAt()) : 0;
			
			EnrollmentAccessStatusDto status = EnrollmentAccessStatusDto.builder()
					.hasAccess(hasAccess)
					.expiresAt(enrollment.getExpiresAt())
					.status(enrollment.getStatus().toString())
					.remainingDays(remainingDays)
					.build();
			
			return ResponseEntity.ok(ApiResponse.success(
					"Access status retrieved",
					status
			));
		} catch (ResourceNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(ApiResponse.error("Enrollment not found"));
		}
	}

	@GetMapping("/summary")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<LearningTimeSummaryDto>> getLearningSummary(Authentication auth) {
		Long studentId = (Long) auth.getCredentials();
		LocalDate now = LocalDate.now();
		LocalDateTime todayStart = now.atStartOfDay();
		LocalDateTime weekStart = now.minusDays(6).atStartOfDay();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d");

		List<com.lms.enrollment.entity.LearningEvent> weekEvents = learningEventRepository
				.findByStudentIdAndCreatedAtBetween(studentId, weekStart, LocalDateTime.now());

		Map<LocalDate, Long> dayTotals = new HashMap<>();
		for (int i = 0; i < 7; i++) {
			dayTotals.put(now.minusDays(i), 0L);
		}

		for (com.lms.enrollment.entity.LearningEvent event : weekEvents) {
			if (event.getEventType() == LearningEventType.VIDEO_WATCH
					|| event.getEventType() == LearningEventType.DOCUMENT_READ
					|| event.getEventType() == LearningEventType.QUIZ_ATTEMPT) {
				Long seconds = event.getValue() != null ? event.getValue().longValue() : 0L;
				LocalDate day = event.getCreatedAt().toLocalDate();
				if (dayTotals.containsKey(day)) {
					dayTotals.put(day, dayTotals.get(day) + seconds);
				}
			}
		}

		List<LearningTimeSummaryDto.DailyLearningTimeDto> dailyTimes = new ArrayList<>();
		for (int i = 6; i >= 0; i--) {
			LocalDate day = now.minusDays(i);
			long seconds = dayTotals.getOrDefault(day, 0L);
			dailyTimes.add(LearningTimeSummaryDto.DailyLearningTimeDto.builder()
					.date(day.format(formatter))
					.seconds(seconds)
					.formattedTime(learningTimeService.formatLearningTime(seconds))
					.build());
		}

		long todaySeconds = dayTotals.getOrDefault(now, 0L);
		long weeklySeconds = dayTotals.values().stream().mapToLong(Long::longValue).sum();
		long totalSeconds = enrollmentService.getMyEnrollments(studentId).stream()
			.mapToLong(e -> e.getTotalTimeSpentSeconds() != null ? e.getTotalTimeSpentSeconds() : 0L)
			.sum();

		LearningTimeSummaryDto summary = LearningTimeSummaryDto.builder()
				.totalTimeSpentSeconds(totalSeconds)
				.todayTimeSpentSeconds(todaySeconds)
				.weeklyTimeSpentSeconds(weeklySeconds)
				.formattedTotalTime(learningTimeService.formatLearningTime(totalSeconds))
				.formattedTodayTime(learningTimeService.formatLearningTime(todaySeconds))
				.formattedWeeklyTime(learningTimeService.formatLearningTime(weeklySeconds))
				.dailyLearningTimes(dailyTimes)
				.build();

		return ResponseEntity.ok(ApiResponse.success("Learning summary fetched", summary));
	}

	private EnrollmentResponseDto toDto(Enrollment enrollment) {
		EnrollmentResponseDto dto = new EnrollmentResponseDto();
		dto.setId(enrollment.getId());
		dto.setStudentId(enrollment.getStudentId());
		dto.setCourseId(enrollment.getCourseId());
		dto.setStatus(enrollment.getStatus());
		dto.setEnrolledAt(enrollment.getEnrolledAt());
		dto.setExpiresAt(enrollment.getExpiresAt());
		dto.setCompletedAt(enrollment.getCompletedAt());
		dto.setAccessDurationDays(enrollment.getAccessDurationDays());
		dto.setTotalTimeSpentSeconds(enrollment.getTotalTimeSpentSeconds());
		dto.setFormattedLearningTime(learningTimeService.formatLearningTime(enrollment.getTotalTimeSpentSeconds()));
		dto.setLastAccessedAt(enrollment.getLastAccessedAt());
		dto.setAccessExpired(enrollment.getExpiresAt() != null && 
				LocalDateTime.now().isAfter(enrollment.getExpiresAt()));
		
		return dto;
	}
}
