package com.lms.course.service.impl;

import java.math.BigDecimal;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lms.notification.service.EmailService;
import com.lms.course.dto.request.CreateCourseRequestDto;
import com.lms.course.dto.request.UpdateCourseRequestDto;
import com.lms.course.dto.response.CourseResponseDto;
import com.lms.course.dto.response.CourseSummaryResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ReviewRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.S3StorageService;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

	private final CourseRepository courseRepository;
	private final LessonRepository lessonRepository;
	private final ReviewRepository reviewRepository;
	private final S3StorageService s3StorageService;
	private final ModelMapper modelMapper;
	private final UserService userService;
	private final EmailService emailService;
	private final EnrollmentRepository enrollmentRepository;

	@Override
	public CourseResponseDto createCourse(Long instructorId, CreateCourseRequestDto request) {
		Course course = Course.builder().instructorId(instructorId).title(request.getTitle())
				.description(request.getDescription())
				.price(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO)
				.currency(request.getCurrency() != null ? request.getCurrency() : "INR").level(request.getLevel())
				.category(request.getCategory()).build();
		Course saved = courseRepository.save(course);

		try {
			UserResponseDto instructor = userService.getUserById(instructorId);
			emailService.sendCourseCreatedEmail(instructor.getEmail(), instructor.getFullName(), saved.getTitle());
		} catch (Exception ignored) {
			// Keep course creation successful even if email delivery fails.
		}

		return toCourseResponseDto(saved);
	}

	@Override
	public Page<CourseSummaryResponseDto> getInstructorCourses(Long instructorId, Pageable pageable) {
		return courseRepository.findByInstructorId(instructorId, pageable).map(this::toCourseSummaryDto);
	}

	@Override
	public Page<CourseSummaryResponseDto> getPublishedCourses(String category, String level, String search, Pageable pageable) {
		String normalizedCategory = (category != null && !category.trim().isEmpty()) ? category.trim() : null;
		String normalizedLevel = (level != null && !level.trim().isEmpty()) ? level.trim() : null;
		String normalizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

		Page<Course> courses;
		boolean hasSearch = normalizedSearch != null;
		
		if (hasSearch) {
			if (normalizedCategory != null && normalizedLevel != null) {
				courses = courseRepository.findByStatusAndCategoryAndLevelAndSearchTerm(CourseStatus.PUBLISHED, normalizedCategory, CourseLevel.valueOf(normalizedLevel.toUpperCase()), normalizedSearch, pageable);
			} else if (normalizedCategory != null) {
				courses = courseRepository.findByStatusAndCategoryAndSearchTerm(CourseStatus.PUBLISHED, normalizedCategory, normalizedSearch, pageable);
			} else if (normalizedLevel != null) {
				courses = courseRepository.findByStatusAndLevelAndSearchTerm(CourseStatus.PUBLISHED, CourseLevel.valueOf(normalizedLevel.toUpperCase()), normalizedSearch, pageable);
			} else {
				courses = courseRepository.findByStatusAndSearchTerm(CourseStatus.PUBLISHED, normalizedSearch, pageable);
			}
		} else {
			if (normalizedCategory != null && normalizedLevel != null) {
				courses = courseRepository.findByStatusAndCategoryAndLevel(CourseStatus.PUBLISHED, normalizedCategory, CourseLevel.valueOf(normalizedLevel.toUpperCase()), pageable);
			} else if (normalizedCategory != null) {
				courses = courseRepository.findByStatusAndCategory(CourseStatus.PUBLISHED, normalizedCategory, pageable);
			} else if (normalizedLevel != null) {
				courses = courseRepository.findByStatusAndLevel(CourseStatus.PUBLISHED, CourseLevel.valueOf(normalizedLevel.toUpperCase()), pageable);
			} else {
				courses = courseRepository.findByStatus(CourseStatus.PUBLISHED, pageable);
			}
		}
		return courses.map(this::toCourseSummaryDto);
	}

	@Override
	public CourseResponseDto getCourseById(Long id) {
		return toCourseResponseDto(getCourseEntityById(id));
	}

	@Override
	public CourseResponseDto updateCourse(Long courseId, Long instructorId, UpdateCourseRequestDto request) {
		Course course = getCourseEntityById(courseId);
		if (!course.getInstructorId().equals(instructorId))
			throw new UnauthorizedException("You do not own this course");
		if (request.getTitle() != null)
			course.setTitle(request.getTitle());
		if (request.getDescription() != null)
			course.setDescription(request.getDescription());
		if (request.getPrice() != null)
			course.setPrice(request.getPrice());
		if (request.getLevel() != null)
			course.setLevel(request.getLevel());
		if (request.getCategory() != null)
			course.setCategory(request.getCategory());
		if (request.getStatus() != null)
			course.setStatus(request.getStatus());
		return toCourseResponseDto(courseRepository.save(course));
	}

	@Override
	public void deleteCourse(Long courseId, Long userId, String role) {
		Course course = getCourseEntityById(courseId);
		if (!role.contains("ADMIN") && !course.getInstructorId().equals(userId))
			throw new UnauthorizedException("Not authorized to delete this course");
		enrollmentRepository.deleteByCourseId(courseId);
		courseRepository.delete(course);
	}

	@Override
	public CourseResponseDto publishCourse(Long courseId, Long instructorId) {
		Course course = getCourseEntityById(courseId);
		if (!course.getInstructorId().equals(instructorId))
			throw new UnauthorizedException("You do not own this course");
		if (course.getStatus() != CourseStatus.DRAFT)
			throw new BadRequestException("Only DRAFT courses can be published");
		course.setStatus(CourseStatus.PUBLISHED);
		return toCourseResponseDto(courseRepository.save(course));
	}

	@Override
	public String uploadThumbnail(Long courseId, Long instructorId, MultipartFile file) {
		Course course = getCourseEntityById(courseId);
		if (!course.getInstructorId().equals(instructorId))
			throw new UnauthorizedException("You do not own this course");
		String url = s3StorageService.uploadFile(file, "thumbnails");
		course.setThumbnailUrl(url);
		courseRepository.save(course);
		return url;
	}

	@Override
	public Course getCourseEntityById(Long id) {
		return courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));
	}

	private CourseResponseDto toCourseResponseDto(Course course) {
		CourseResponseDto dto = modelMapper.map(course, CourseResponseDto.class);
		dto.setThumbnailUrl(s3StorageService.getAccessibleFileUrl(dto.getThumbnailUrl()));
		dto.setAverageRating(reviewRepository.findAverageRatingByCourseId(course.getId()));
		dto.setTotalReviews(reviewRepository.countByCourseId(course.getId()));
		dto.setTotalLessons(lessonRepository.countByCourseId(course.getId()));
		return dto;
	}

	private CourseSummaryResponseDto toCourseSummaryDto(Course course) {
		CourseSummaryResponseDto dto = modelMapper.map(course, CourseSummaryResponseDto.class);
		dto.setThumbnailUrl(s3StorageService.getAccessibleFileUrl(dto.getThumbnailUrl()));
		dto.setAverageRating(reviewRepository.findAverageRatingByCourseId(course.getId()));
		dto.setTotalLessons(lessonRepository.countByCourseId(course.getId()));
		return dto;
	}
}