package com.lms.enrollment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	@PostMapping
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<EnrollmentResponseDto>> enroll(Authentication auth,
			@Valid @RequestBody EnrollRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Enrolled successfully", enrollmentService.enroll(userId, request)));
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<List<EnrollmentResponseDto>>> getMyEnrollments(Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Enrollments fetched", enrollmentService.getMyEnrollments(userId)));
	}

	@GetMapping("/course/{courseId}/check")
	public ResponseEntity<ApiResponse<Boolean>> checkEnrollment(@PathVariable Long courseId, Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Enrollment check", enrollmentService.isEnrolled(userId, courseId)));
	}

	@GetMapping("/course/{courseId}/students")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<List<EnrollmentResponseDto>>> getCourseStudents(@PathVariable Long courseId) {
		return ResponseEntity
				.ok(ApiResponse.success("Students fetched", enrollmentService.getStudentsByCourse(courseId)));
	}
}