package com.lms.enrollment.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import com.lms.course.entity.Course;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.S3StorageService;
import com.lms.course.vo.CourseLevel;
import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.enrollment.service.LearningTimeService;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.service.EmailService;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Slf4j
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
	private final LearningTimeService learningTimeService;

	@Override
	public EnrollmentResponseDto enroll(Long studentId, EnrollRequestDto request) {
		return createEnrollmentDirectly(studentId, request.getCourseId());
	}

	@Override
	public EnrollmentResponseDto createEnrollmentDirectly(Long studentId, Long courseId) {
		if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId))
			throw new BadRequestException("Already enrolled in this course");
		Course course = courseService.getCourseEntityById(courseId); // validate course exists
		LocalDateTime enrolledAt = LocalDateTime.now();
		LocalDateTime expiresAt = calculateExpiryDate(enrolledAt, course.getLevel());
		Integer accessDurationDays = calculateAccessDurationDays(course.getLevel());
		
		Enrollment enrollment = Enrollment.builder()
				.studentId(studentId)
				.courseId(courseId)
				.enrolledAt(enrolledAt)
				.expiresAt(expiresAt)
				.accessDurationDays(accessDurationDays)
				.status(EnrollmentStatus.ACTIVE)
				.totalTimeSpentSeconds(0L)
				.progressPercentage(0.0)
				.build();
		
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
		return enrollmentRepository.findByStudentId(studentId).stream().map(enrollment -> {
			try {
				courseService.getCourseEntityById(enrollment.getCourseId());
				return toDto(ensureExpiryFields(enrollment));
			} catch (ResourceNotFoundException e) {
				log.info("Deleting orphan enrollment {} for missing course {}", enrollment.getId(), enrollment.getCourseId());
				enrollmentRepository.delete(enrollment);
				return null;
			}
		}).filter(Objects::nonNull).toList();
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
				.map(this::ensureExpiryFields)
				.orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
	}

	private Enrollment ensureExpiryFields(Enrollment enrollment) {
		if (enrollment == null) {
			return null;
		}

		boolean updated = false;
		if (enrollment.getExpiresAt() == null && enrollment.getEnrolledAt() != null) {
			try {
				Course course = courseService.getCourseEntityById(enrollment.getCourseId());
				LocalDateTime expiresAt = calculateExpiryDate(enrollment.getEnrolledAt(), course.getLevel());
				Integer accessDurationDays = calculateAccessDurationDays(course.getLevel());
				enrollment.setExpiresAt(expiresAt);
				enrollment.setAccessDurationDays(accessDurationDays);
				updated = true;
			} catch (Exception e) {
				log.warn("Cannot compute expiry for enrollment {}: {}", enrollment.getId(), e.getMessage());
			}
		}

		if (enrollment.getAccessDurationDays() == null && enrollment.getExpiresAt() != null && enrollment.getEnrolledAt() != null) {
			try {
				Course course = courseService.getCourseEntityById(enrollment.getCourseId());
				Integer accessDurationDays = calculateAccessDurationDays(course.getLevel());
				enrollment.setAccessDurationDays(accessDurationDays);
				updated = true;
			} catch (Exception e) {
				log.warn("Cannot compute access duration for enrollment {}: {}", enrollment.getId(), e.getMessage());
			}
		}

		if (updated) {
			enrollmentRepository.save(enrollment);
		}

		return enrollment;
	}

	private EnrollmentResponseDto toDto(Enrollment enrollment) {
		EnrollmentResponseDto dto = modelMapper.map(enrollment, EnrollmentResponseDto.class);
		
		// Check if course access has expired
		boolean accessExpired = enrollment.getExpiresAt() != null && LocalDateTime.now().isAfter(enrollment.getExpiresAt());
		dto.setAccessExpired(accessExpired);
		
		// Update status to EXPIRED if access has expired
		if (accessExpired && !enrollment.getStatus().equals(EnrollmentStatus.EXPIRED)) {
			enrollment.setStatus(EnrollmentStatus.EXPIRED);
			enrollmentRepository.save(enrollment);
			dto.setStatus(EnrollmentStatus.EXPIRED);
		}
		
		if (enrollment.getExpiresAt() != null) {
			long remainingDays = ChronoUnit.DAYS.between(LocalDateTime.now(), enrollment.getExpiresAt());
			dto.setRemainingDays(Math.max(remainingDays, 0));
		}
		
		// Format learning time
		String formattedTime = learningTimeService.formatLearningTime(enrollment.getTotalTimeSpentSeconds());
		dto.setFormattedLearningTime(formattedTime);
		
		try {
			// Try to fetch course details
			try {
				Course course = courseService.getCourseEntityById(enrollment.getCourseId());
				dto.setCourseTitle(course.getTitle());
				try {
					dto.setCourseThumbnailUrl(s3StorageService.getAccessibleFileUrl(course.getThumbnailUrl()));
				} catch (Exception e) {
					log.warn("Failed to get thumbnail URL for course {}: {}", enrollment.getCourseId(), e.getMessage());
					dto.setCourseThumbnailUrl(null);
				}
			} catch (Exception e) {
				log.warn("Course not found for enrollment {}: {}", enrollment.getId(), e.getMessage());
				dto.setCourseTitle("Course #" + enrollment.getCourseId());
			}
			
			// Calculate completion percentage
			try {
				long total = lessonRepository.countByCourseId(enrollment.getCourseId());
				long completed = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());

				if (total > 0 && completed >= total && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
					enrollment.setStatus(EnrollmentStatus.COMPLETED);
					enrollment.setCompletedAt(LocalDateTime.now());
					enrollmentRepository.save(enrollment);
					dto.setStatus(EnrollmentStatus.COMPLETED);
					dto.setCompletedAt(enrollment.getCompletedAt());
				} else if (total > 0 && completed < total && enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
					enrollment.setStatus(EnrollmentStatus.ACTIVE);
					enrollment.setCompletedAt(null);
					enrollmentRepository.save(enrollment);
					dto.setStatus(EnrollmentStatus.ACTIVE);
					dto.setCompletedAt(null);
				}

				dto.setCompletionPercentage(total > 0 ? (completed * 100.0 / total) : 0.0);
			} catch (Exception e) {
				log.warn("Failed to calculate progress for enrollment {}: {}", enrollment.getId(), e.getMessage());
				dto.setCompletionPercentage(0.0);
			}
		} catch (Exception e) {
			log.error("Error converting enrollment {} to DTO: {}", enrollment.getId(), e.getMessage(), e);
		}
		
		return dto;
	}

	private LocalDateTime calculateExpiryDate(LocalDateTime enrolledAt, CourseLevel level) {
		if (level == null) {
			return enrolledAt.plusMonths(6);
		}
		return switch (level) {
		case BEGINNER -> enrolledAt.plusMonths(6);
		case INTERMEDIATE -> enrolledAt.plusMonths(9);
		case ADVANCED -> enrolledAt.plusYears(1);
		};
	}

	private Integer calculateAccessDurationDays(CourseLevel level) {
		if (level == null) {
			return 180; // 6 months default
		}
		return switch (level) {
		case BEGINNER -> 180; // 6 months
		case INTERMEDIATE -> 270; // 9 months
		case ADVANCED -> 365; // 12 months
		};
	}
}