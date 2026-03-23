package com.lms.course.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.lms.course.dto.request.SubmitReviewRequestDto;
import com.lms.course.dto.response.ReviewResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Review;
import com.lms.course.repository.ReviewRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.ReviewService;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final CourseService courseService;
	private final EnrollmentRepository enrollmentRepository;
	private final ModelMapper modelMapper;
	private final UserRepository userRepository;

	@Override
	public ReviewResponseDto submitReview(Long courseId, Long studentId, SubmitReviewRequestDto request) {
		if (!enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
			throw new BadRequestException("You must be enrolled to review this course");
		if (reviewRepository.existsByCourseIdAndStudentId(courseId, studentId))
			throw new BadRequestException("You have already reviewed this course");

		Course course = courseService.getCourseEntityById(courseId);
		Review review = Review.builder().course(course).studentId(studentId).rating(request.getRating())
				.comment(request.getComment()).build();
		Review saved = reviewRepository.save(review);
		ReviewResponseDto dto = modelMapper.map(saved, ReviewResponseDto.class);
		dto.setCourseId(courseId);
		dto.setStudentName(userRepository.findById(studentId).map(u -> u.getFullName()).orElse(null));
		return dto;
	}

	@Override
	public List<ReviewResponseDto> getReviewsByCourse(Long courseId) {
		return reviewRepository.findByCourseId(courseId).stream().map(r -> {
			ReviewResponseDto dto = modelMapper.map(r, ReviewResponseDto.class);
			dto.setCourseId(courseId);
			dto.setStudentName(userRepository.findById(r.getStudentId()).map(u -> u.getFullName()).orElse(null));
			return dto;
		}).toList();
	}

	@Override
	public Double getAverageRating(Long courseId) {
		Double avg = reviewRepository.findAverageRatingByCourseId(courseId);
		return avg != null ? avg : 0.0;
	}

	@Override
	public void deleteReview(Long reviewId) {
		if (!reviewRepository.existsById(reviewId))
			throw new ResourceNotFoundException("Review", reviewId);
		reviewRepository.deleteById(reviewId);
	}
}