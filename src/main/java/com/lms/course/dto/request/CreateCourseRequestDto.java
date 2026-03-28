package com.lms.course.dto.request;

import java.math.BigDecimal;

import com.lms.course.vo.CourseLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourseRequestDto {
	@NotBlank(message = "Title is required")
	private String title;
	private String description;
	private BigDecimal price;
	private String currency;
	@NotNull(message = "Level is required")
	private CourseLevel level;
	private String category;
}