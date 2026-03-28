package com.lms.course.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateLessonRequestDto;
import com.lms.course.dto.response.LessonResponseDto;
import com.lms.course.entity.Lesson;

public interface LessonService {
	LessonResponseDto createLesson(Long instructorId, CreateLessonRequestDto request, MultipartFile file);

	List<LessonResponseDto> getLessonsByCourse(Long courseId, Long userId, String role);

	LessonResponseDto updateLesson(Long lessonId, Long instructorId, CreateLessonRequestDto request,
			MultipartFile file);

	void deleteLesson(Long lessonId, Long instructorId);

	Lesson getLessonEntityById(Long id);
}