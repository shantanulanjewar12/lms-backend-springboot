package com.lms.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lms.analytics.dto.response.LearningPathResponseDto;
import com.lms.analytics.dto.response.StudentProgressSummaryDto;
import com.lms.analytics.service.StudentAnalyticsService;
import com.lms.shared.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class StudentAnalyticsController {

	private final StudentAnalyticsService studentAnalyticsService;

	@GetMapping("/students/{id}/summary")
	@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
	public ResponseEntity<ApiResponse<StudentProgressSummaryDto>> getStudentSummary(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Student summary", studentAnalyticsService.getStudentSummary(id)));
	}

	@GetMapping("/learning-path")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<LearningPathResponseDto>> getLearningPath(@RequestParam String goal,
			Authentication auth) {
		Long studentId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Learning path", studentAnalyticsService.getLearningPath(studentId, goal)));
	}
}