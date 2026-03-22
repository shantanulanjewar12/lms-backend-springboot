package com.lms.enrollment.dto.response;

import java.time.LocalDateTime;

import com.lms.enrollment.vo.EnrollmentStatus;

import lombok.Data;

@Data
public class EnrollmentResponseDto {
	private Long id;
	private Long studentId;
	private Long courseId;
	private String courseTitle;
	private String courseThumbnailUrl;
	private EnrollmentStatus status;
	private LocalDateTime enrolledAt;
	private LocalDateTime completedAt;
	private Double completionPercentage;
}