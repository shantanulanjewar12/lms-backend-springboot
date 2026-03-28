package com.lms.course.dto.request;

import java.math.BigDecimal;

import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;

import lombok.Data;

@Data
public class UpdateCourseRequestDto {
	private String title;
	private String description;
	private BigDecimal price;
	private CourseLevel level;
	private String category;
	private CourseStatus status;
}