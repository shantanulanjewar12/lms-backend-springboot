package com.lms.course.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.course.dto.request.SubmitReviewRequestDto;
import com.lms.course.dto.response.ReviewResponseDto;
import com.lms.course.service.ReviewService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping("/{id}/reviews")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<ReviewResponseDto>> submitReview(@PathVariable Long id, Authentication auth,
			@Valid @RequestBody SubmitReviewRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Review submitted", reviewService.submitReview(id, userId, request)));
	}

	@GetMapping("/{id}/reviews")
	public ResponseEntity<ApiResponse<List<ReviewResponseDto>>> getReviews(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Reviews fetched", reviewService.getReviewsByCourse(id)));
	}

	@GetMapping("/{id}/reviews/average")
	public ResponseEntity<ApiResponse<Double>> getAverageRating(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Average rating", reviewService.getAverageRating(id)));
	}

	@DeleteMapping("/{courseId}/reviews/{reviewId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
		reviewService.deleteReview(reviewId);
		return ResponseEntity.ok(ApiResponse.success("Review deleted"));
	}
}