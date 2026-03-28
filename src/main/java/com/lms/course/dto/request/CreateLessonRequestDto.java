package com.lms.course.dto.request;

import com.lms.course.vo.ContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateLessonRequestDto {
	@NotNull
	private Long courseId;
	@NotBlank
	private String title;
	@NotNull
	private ContentType contentType;
	@NotNull
	private Integer orderIndex;
	private Boolean isFreePreview = false;
	private Integer durationSeconds;
}