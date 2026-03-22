package com.lms.course.service;

import java.util.List;

import com.lms.course.dto.request.SubmitReviewRequestDto;
import com.lms.course.dto.response.ReviewResponseDto;

public interface ReviewService {
	ReviewResponseDto submitReview(Long courseId, Long studentId, SubmitReviewRequestDto request);

	List<ReviewResponseDto> getReviewsByCourse(Long courseId);

	Double getAverageRating(Long courseId);

	void deleteReview(Long reviewId);
}