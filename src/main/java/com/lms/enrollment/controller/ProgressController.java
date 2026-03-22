package com.lms.enrollment.controller;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.lms.enrollment.dto.request.LearningEventRequestDto;
import com.lms.enrollment.dto.request.LessonProgressRequestDto;
import com.lms.enrollment.dto.response.LearningEventResponseDto;
import com.lms.enrollment.dto.response.ProgressResponseDto;
import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.service.ProgressService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ProgressController {

	private final ProgressService progressService;
	private final LearningEventRepository learningEventRepository;
	private final ModelMapper modelMapper;

	@PutMapping("/api/v1/progress/lesson")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<ProgressResponseDto>> updateProgress(Authentication auth,
			@Valid @RequestBody LessonProgressRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Progress updated", progressService.updateLessonProgress(userId, request)));
	}

	@GetMapping("/api/v1/progress/course/{courseId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<ProgressResponseDto>> getCourseProgress(@PathVariable Long courseId,
			Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Progress fetched", progressService.getCourseProgress(userId, courseId)));
	}

	@PostMapping("/api/v1/events")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<LearningEventResponseDto>> logEvent(Authentication auth,
			@Valid @RequestBody LearningEventRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		LearningEvent event = LearningEvent.builder().studentId(userId).lessonId(request.getLessonId())
				.eventType(request.getEventType()).value(request.getValue()).build();
		LearningEvent saved = learningEventRepository.save(event);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Event logged", modelMapper.map(saved, LearningEventResponseDto.class)));
	}

	@GetMapping("/api/v1/events/lesson/{lessonId}")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<List<LearningEventResponseDto>>> getEventsByLesson(@PathVariable Long lessonId) {
		List<LearningEventResponseDto> events = learningEventRepository.findByLessonId(lessonId).stream()
				.map(e -> modelMapper.map(e, LearningEventResponseDto.class)).toList();
		return ResponseEntity.ok(ApiResponse.success("Events fetched", events));
	}
}