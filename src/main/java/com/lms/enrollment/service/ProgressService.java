package com.lms.enrollment.service;

import com.lms.enrollment.dto.request.LessonProgressRequestDto;
import com.lms.enrollment.dto.response.ProgressResponseDto;

public interface ProgressService {
	ProgressResponseDto updateLessonProgress(Long studentId, LessonProgressRequestDto request);

	ProgressResponseDto getCourseProgress(Long studentId, Long courseId);
}