package com.lms.course.dto.response;

import java.math.BigDecimal;

import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;

import lombok.Data;

@Data
public class CourseSummaryResponseDto {
	private Long id;
	private String title;
	private String thumbnailUrl;
	private BigDecimal price;
	private CourseLevel level;
	private CourseStatus status;
	private Double averageRating;
	private Long totalLessons;
	private String category;
}