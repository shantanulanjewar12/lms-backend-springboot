package com.lms.enrollment.service;

import java.util.List;

import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.entity.Enrollment;

public interface EnrollmentService {
	EnrollmentResponseDto enroll(Long studentId, EnrollRequestDto request);

	EnrollmentResponseDto createEnrollmentDirectly(Long studentId, Long courseId);

	List<EnrollmentResponseDto> getMyEnrollments(Long studentId);

	boolean isEnrolled(Long studentId, Long courseId);

	List<EnrollmentResponseDto> getStudentsByCourse(Long courseId);

	Enrollment getEnrollmentEntity(Long studentId, Long courseId);
}