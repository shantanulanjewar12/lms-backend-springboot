package com.lms.enrollment.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.lms.course.entity.Course;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.S3StorageService;
import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.notification.service.EmailService;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

	private final EnrollmentRepository enrollmentRepository;
	private final LessonRepository lessonRepository;
	private final LessonProgressRepository lessonProgressRepository;
	private final CourseService courseService;
	private final S3StorageService s3StorageService;
	private final ModelMapper modelMapper;
	private final UserService userService;
	private final EmailService emailService;

	@Override
	public EnrollmentResponseDto enroll(Long studentId, EnrollRequestDto request) {
		return createEnrollmentDirectly(studentId, request.getCourseId());
	}

	@Override
	public EnrollmentResponseDto createEnrollmentDirectly(Long studentId, Long courseId) {
		if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
			throw new BadRequestException("Already enrolled in this course");
		Course course = courseService.getCourseEntityById(courseId); // validate course exists
		Enrollment enrollment = Enrollment.builder().studentId(studentId).courseId(courseId).build();
		Enrollment saved = enrollmentRepository.save(enrollment);

		try {
			UserResponseDto student = userService.getUserById(studentId);
			emailService.sendEnrollmentEmail(student.getEmail(), student.getFullName(), course.getTitle());
		} catch (Exception ignored) {
			// Enrollment should not fail due to email delivery issues.
		}

		return toDto(saved);
	}

	@Override
	public List<EnrollmentResponseDto> getMyEnrollments(Long studentId) {
		return enrollmentRepository.findByStudentId(studentId).stream().map(this::toDto).toList();
	}

	@Override
	public boolean isEnrolled(Long studentId, Long courseId) {
		return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
	}

	@Override
	public List<EnrollmentResponseDto> getStudentsByCourse(Long courseId) {
		return enrollmentRepository.findByCourseId(courseId).stream().map(this::toDto).toList();
	}

	@Override
	public Enrollment getEnrollmentEntity(Long studentId, Long courseId) {
		return enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
	}

	private EnrollmentResponseDto toDto(Enrollment enrollment) {
		EnrollmentResponseDto dto = modelMapper.map(enrollment, EnrollmentResponseDto.class);
		Course course = courseService.getCourseEntityById(enrollment.getCourseId());
		dto.setCourseTitle(course.getTitle());
		dto.setCourseThumbnailUrl(s3StorageService.getAccessibleFileUrl(course.getThumbnailUrl()));
		long total = lessonRepository.countByCourseId(enrollment.getCourseId());
		long completed = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());
		dto.setCompletionPercentage(total > 0 ? (completed * 100.0 / total) : 0.0);
		return dto;
	}
}