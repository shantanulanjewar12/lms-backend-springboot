package com.lms.analytics.service;

import java.util.List;

import com.lms.analytics.dto.response.LessonDifficultyResponseDto;

public interface LessonDifficultyService {
	List<LessonDifficultyResponseDto> getDifficultyForCourse(Long courseId);
}