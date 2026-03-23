package com.lms.course.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReviewResponseDto {
	private Long id;
	private Long courseId;
	private Long studentId;
	private String studentName;
	private Short rating;
	private String comment;
	private LocalDateTime createdAt;
}