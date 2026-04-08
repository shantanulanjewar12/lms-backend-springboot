package com.lms.course.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateLessonRequestDto;
import com.lms.course.dto.request.QuizQuestionUpsertRequestDto;
import com.lms.course.dto.request.QuizSubmitRequestDto;
import com.lms.course.dto.response.QuizAttemptResponseDto;
import com.lms.course.dto.response.LessonResponseDto;
import com.lms.course.dto.response.QuizQuestionResponseDto;
import com.lms.course.dto.response.QuizSubmitResponseDto;
import com.lms.course.service.LessonService;
import com.lms.course.service.QuizService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessonService;
	private final QuizService quizService;

	@PostMapping(value = "/api/v1/lessons", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<LessonResponseDto>> createLesson(Authentication auth,
			@RequestPart("data") CreateLessonRequestDto request,
			@RequestPart(value = "file", required = false) MultipartFile file) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Lesson created", lessonService.createLesson(userId, request, file)));
	}

	@GetMapping("/api/v1/courses/{id}/lessons")
	public ResponseEntity<ApiResponse<List<LessonResponseDto>>> getLessons(@PathVariable Long id, Authentication auth) {
		Long userId = auth != null ? (Long) auth.getCredentials() : null;
		String role = auth != null ? auth.getAuthorities().iterator().next().getAuthority() : "PUBLIC";
		return ResponseEntity
				.ok(ApiResponse.success("Lessons fetched", lessonService.getLessonsByCourse(id, userId, role)));
	}

	@PutMapping(value = "/api/v1/lessons/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<LessonResponseDto>> updateLesson(@PathVariable Long id, Authentication auth,
			@RequestPart("data") CreateLessonRequestDto request,
			@RequestPart(value = "file", required = false) MultipartFile file) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Lesson updated", lessonService.updateLesson(id, userId, request, file)));
	}

	@DeleteMapping("/api/v1/lessons/{id}")
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long id, Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		lessonService.deleteLesson(id, userId);
		return ResponseEntity.ok(ApiResponse.success("Lesson deleted"));
	}

	@PutMapping("/api/v1/lessons/{id}/quiz")
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<List<QuizQuestionResponseDto>>> upsertQuizQuestions(@PathVariable Long id,
			Authentication auth, @Valid @RequestBody List<QuizQuestionUpsertRequestDto> request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Quiz questions saved", quizService.upsertQuizQuestions(userId, id, request)));
	}

	@GetMapping("/api/v1/lessons/{id}/quiz")
	@PreAuthorize("hasAnyRole('STUDENT','INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<List<QuizQuestionResponseDto>>> getQuizQuestions(@PathVariable Long id,
			Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		String role = auth.getAuthorities().iterator().next().getAuthority();
		return ResponseEntity.ok(ApiResponse.success("Quiz questions fetched", quizService.getQuizQuestions(id, userId, role)));
	}

	@PostMapping("/api/v1/lessons/{id}/quiz/submit")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<QuizSubmitResponseDto>> submitQuiz(@PathVariable Long id, Authentication auth,
			@Valid @RequestBody QuizSubmitRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Quiz submitted", quizService.submitQuiz(userId, id, request)));
	}

	@GetMapping("/api/v1/lessons/{id}/quiz/attempts")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<List<QuizAttemptResponseDto>>> getQuizAttempts(@PathVariable Long id,
			Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Quiz attempts fetched", quizService.getQuizAttempts(userId, id)));
	}

	@GetMapping("/api/v1/lessons/{id}/quiz/attempts/latest")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<QuizAttemptResponseDto>> getLatestQuizAttempt(@PathVariable Long id,
			Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Latest quiz attempt fetched", quizService.getLatestQuizAttempt(userId, id)));
	}
}