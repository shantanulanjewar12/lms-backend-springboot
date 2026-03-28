package com.lms.course.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import com.lms.course.dto.request.SubmitReviewRequestDto;
import com.lms.course.dto.response.ReviewResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Review;
import com.lms.course.repository.ReviewRepository;
import com.lms.course.service.CourseService;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private static final Long COURSE_ID = 1L;
    private static final Long STUDENT_ID = 10L;
    private static final Long REVIEW_ID = 100L;

    private Course course;
    private Review review;
    private User student;
    private SubmitReviewRequestDto submitRequest;
    private ReviewResponseDto reviewResponseDto;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(COURSE_ID)
                .title("Spring Boot Mastery")
                .build();

        review = Review.builder()
                .id(REVIEW_ID)
                .course(course)
                .studentId(STUDENT_ID)
                .rating((short) 4)
                .comment("Great course!")
                .build();

        student = new User();
        student.setId(STUDENT_ID);
        student.setFullName("Alice Smith");

        submitRequest = new SubmitReviewRequestDto();
        submitRequest.setRating((short) 4);
        submitRequest.setComment("Great course!");

        reviewResponseDto = new ReviewResponseDto();
        reviewResponseDto.setId(REVIEW_ID);
        reviewResponseDto.setRating((short) 4);
        reviewResponseDto.setComment("Great course!");
    }

    @Nested
    @DisplayName("submitReview()")
    class SubmitReview {

        @Test
        @DisplayName("Should submit review successfully when student is enrolled and has not reviewed before")
        void submitReview_success() {
            when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);
            when(reviewRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(false);
            when(courseService.getCourseEntityById(COURSE_ID)).thenReturn(course);
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(modelMapper.map(review, ReviewResponseDto.class)).thenReturn(reviewResponseDto);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            ReviewResponseDto result = reviewService.submitReview(COURSE_ID, STUDENT_ID, submitRequest);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(REVIEW_ID);
            assertThat(result.getRating()).isEqualTo((short) 4);
            assertThat(result.getComment()).isEqualTo("Great course!");
            assertThat(result.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(result.getStudentName()).isEqualTo("Alice Smith");

            verify(reviewRepository).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when student is not enrolled")
        void submitReview_notEnrolled_throwsBadRequest() {
            when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.submitReview(COURSE_ID, STUDENT_ID, submitRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("enrolled");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when student has already reviewed the course")
        void submitReview_alreadyReviewed_throwsBadRequest() {
            when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);
            when(reviewRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.submitReview(COURSE_ID, STUDENT_ID, submitRequest))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already reviewed");

            verify(reviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set studentName to null when user is not found in UserRepository")
        void submitReview_userNotFound_studentNameIsNull() {
            when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);
            when(reviewRepository.existsByCourseIdAndStudentId(COURSE_ID, STUDENT_ID)).thenReturn(false);
            when(courseService.getCourseEntityById(COURSE_ID)).thenReturn(course);
            when(reviewRepository.save(any(Review.class))).thenReturn(review);
            when(modelMapper.map(review, ReviewResponseDto.class)).thenReturn(reviewResponseDto);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            ReviewResponseDto result = reviewService.submitReview(COURSE_ID, STUDENT_ID, submitRequest);

            assertThat(result).isNotNull();
            assertThat(result.getStudentName()).isNull();
        }
    }

    @Nested
    @DisplayName("getReviewsByCourse()")
    class GetReviewsByCourse {

        @Test
        @DisplayName("Should return list of ReviewResponseDtos for a course")
        void getReviewsByCourse_returnsList() {
            when(reviewRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(review));
            when(modelMapper.map(review, ReviewResponseDto.class)).thenReturn(reviewResponseDto);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

            List<ReviewResponseDto> result = reviewService.getReviewsByCourse(COURSE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(REVIEW_ID);
            assertThat(result.get(0).getRating()).isEqualTo((short) 4);
            assertThat(result.get(0).getComment()).isEqualTo("Great course!");
            assertThat(result.get(0).getCourseId()).isEqualTo(COURSE_ID);
            assertThat(result.get(0).getStudentName()).isEqualTo("Alice Smith");
        }

        @Test
        @DisplayName("Should return empty list when no reviews exist for the course")
        void getReviewsByCourse_noReviews_returnsEmptyList() {
            when(reviewRepository.findByCourseId(COURSE_ID)).thenReturn(List.of());

            List<ReviewResponseDto> result = reviewService.getReviewsByCourse(COURSE_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should set studentName to null when reviewer user is not found")
        void getReviewsByCourse_userNotFound_studentNameIsNull() {
            when(reviewRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(review));
            when(modelMapper.map(review, ReviewResponseDto.class)).thenReturn(reviewResponseDto);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.empty());

            List<ReviewResponseDto> result = reviewService.getReviewsByCourse(COURSE_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStudentName()).isNull();
        }

        @Test
        @DisplayName("Should map multiple reviews correctly")
        void getReviewsByCourse_multipleReviews() {
            Long student2Id = 20L;

            Review review2 = Review.builder()
                    .id(101L)
                    .course(course)
                    .studentId(student2Id)
                    .rating((short) 5)
                    .comment("Excellent!")
                    .build();

            ReviewResponseDto dto2 = new ReviewResponseDto();
            dto2.setId(101L);
            dto2.setRating((short) 5);
            dto2.setComment("Excellent!");

            User student2 = new User();
            student2.setId(student2Id);
            student2.setFullName("Bob Jones");

            when(reviewRepository.findByCourseId(COURSE_ID)).thenReturn(List.of(review, review2));
            when(modelMapper.map(review, ReviewResponseDto.class)).thenReturn(reviewResponseDto);
            when(modelMapper.map(review2, ReviewResponseDto.class)).thenReturn(dto2);
            when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
            when(userRepository.findById(student2Id)).thenReturn(Optional.of(student2));

            List<ReviewResponseDto> result = reviewService.getReviewsByCourse(COURSE_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStudentName()).isEqualTo("Alice Smith");
            assertThat(result.get(1).getStudentName()).isEqualTo("Bob Jones");
            assertThat(result.get(1).getRating()).isEqualTo((short) 5);
        }
    }

    @Nested
    @DisplayName("getAverageRating()")
    class GetAverageRating {

        @Test
        @DisplayName("Should return average rating from repository")
        void getAverageRating_returnsAverage() {
            when(reviewRepository.findAverageRatingByCourseId(COURSE_ID)).thenReturn(4.2);

            Double result = reviewService.getAverageRating(COURSE_ID);

            assertThat(result).isEqualTo(4.2);
        }

        @Test
        @DisplayName("Should return 0.0 when repository returns null")
        void getAverageRating_nullFromRepo_returnsZero() {
            when(reviewRepository.findAverageRatingByCourseId(COURSE_ID)).thenReturn(null);

            Double result = reviewService.getAverageRating(COURSE_ID);

            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should return 0.0 when average is exactly 0")
        void getAverageRating_zeroAverage() {
            when(reviewRepository.findAverageRatingByCourseId(COURSE_ID)).thenReturn(0.0);

            Double result = reviewService.getAverageRating(COURSE_ID);

            assertThat(result).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("deleteReview()")
    class DeleteReview {

        @Test
        @DisplayName("Should delete review successfully when review exists")
        void deleteReview_success() {
            when(reviewRepository.existsById(REVIEW_ID)).thenReturn(true);

            assertThatNoException().isThrownBy(() -> reviewService.deleteReview(REVIEW_ID));

            verify(reviewRepository).deleteById(REVIEW_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review does not exist")
        void deleteReview_notFound_throwsResourceNotFoundException() {
            when(reviewRepository.existsById(REVIEW_ID)).thenReturn(false);

            assertThatThrownBy(() -> reviewService.deleteReview(REVIEW_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(reviewRepository, never()).deleteById(any());
        }
    }
}