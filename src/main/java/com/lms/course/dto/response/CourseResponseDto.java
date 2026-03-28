package com.lms.course.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;

import lombok.Data;

@Data
public class CourseResponseDto {
	private Long id;
	private String title;
	private String description;
	private String thumbnailUrl;
	private BigDecimal price;
	private String currency;
	private CourseLevel level;
	private CourseStatus status;
	private Long instructorId;
	private Double averageRating;
	private Long totalReviews;
	private Long totalLessons;
	private String category;
	private LocalDateTime createdAt;
}