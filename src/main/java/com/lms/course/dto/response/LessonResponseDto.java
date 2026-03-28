package com.lms.course.dto.response;

import java.time.LocalDateTime;

import com.lms.course.vo.ContentType;

import lombok.Data;

@Data
public class LessonResponseDto {
	private Long id;
	private Long courseId;
	private String title;
	private String contentUrl;
	private ContentType contentType;
	private Integer durationSeconds;
	private Integer orderIndex;
	private Boolean isFreePreview;
	private LocalDateTime createdAt;
}