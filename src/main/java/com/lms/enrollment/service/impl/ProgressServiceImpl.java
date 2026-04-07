package com.lms.enrollment.service.impl;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lms.course.repository.LessonRepository;
import com.lms.enrollment.dto.request.LessonProgressRequestDto;
import com.lms.enrollment.dto.response.ProgressResponseDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LessonProgress;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.service.ProgressService;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.service.BadgeService;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.vo.BadgeType;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgressServiceImpl implements ProgressService {

	private final EnrollmentRepository enrollmentRepository;
	private final LessonProgressRepository lessonProgressRepository;
	private final LessonRepository lessonRepository;
	private final BadgeService badgeService;
	private final StudentBadgeRepository badgeRepository;
	private final LearningStreakRepository streakRepository;

	@Override
	public ProgressResponseDto updateLessonProgress(Long studentId, LessonProgressRequestDto request) {
		Enrollment enrollment = enrollmentRepository
				.findByStudentIdAndCourseId(studentId,
						lessonRepository.findById(request.getLessonId())
								.orElseThrow(() -> new ResourceNotFoundException("Lesson", request.getLessonId()))
								.getCourse().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

		validateEnrollmentAccess(enrollment);

		LessonProgress progress = lessonProgressRepository
				.findByEnrollmentIdAndLessonId(enrollment.getId(), request.getLessonId()).orElse(LessonProgress
						.builder().enrollmentId(enrollment.getId()).lessonId(request.getLessonId()).build());

		progress.setIsCompleted(request.getIsCompleted());
		progress.setWatchDurationSeconds(request.getWatchDurationSeconds());
		progress.setLastWatchedAt(LocalDateTime.now());
		lessonProgressRepository.save(progress);
		
		// Award badges when lesson is completed
		if (request.getIsCompleted()) {
			updateLearningStreak(studentId);
			awardLessonCompletionBadges(studentId, enrollment);
		}

		return buildProgressResponse(enrollment);
	}

	@Override
	public ProgressResponseDto getCourseProgress(Long studentId, Long courseId) {
		Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
				.orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));
		validateEnrollmentAccess(enrollment);
		return buildProgressResponse(enrollment);
	}

	private void validateEnrollmentAccess(Enrollment enrollment) {
		if (enrollment.getExpiresAt() != null && LocalDateTime.now().isAfter(enrollment.getExpiresAt())) {
			throw new UnauthorizedException("Your course access period has expired.");
		}
	}

	private ProgressResponseDto buildProgressResponse(Enrollment enrollment) {
		List<LessonProgress> progressList = lessonProgressRepository.findByEnrollmentId(enrollment.getId());
		long total = lessonRepository.countByCourseId(enrollment.getCourseId());
		long completed = progressList.stream().filter(LessonProgress::getIsCompleted).count();

		ProgressResponseDto dto = new ProgressResponseDto();
		dto.setEnrollmentId(enrollment.getId());
		dto.setCourseId(enrollment.getCourseId());
		dto.setCompletionPercentage(total > 0 ? (completed * 100.0 / total) : 0.0);
		dto.setLessonProgressList(progressList.stream().map(p -> {
			ProgressResponseDto.LessonProgressItemDto item = new ProgressResponseDto.LessonProgressItemDto();
			item.setLessonId(p.getLessonId());
			item.setIsCompleted(p.getIsCompleted());
			item.setWatchDurationSeconds(p.getWatchDurationSeconds());
			return item;
		}).toList());
		return dto;
	}
	
	private void awardLessonCompletionBadges(Long studentId, Enrollment enrollment) {
		// Get count of completed lessons for this student (across all courses)
		List<com.lms.enrollment.entity.LessonProgress> allStudentProgress = lessonProgressRepository
				.findAll()
				.stream()
				.filter(lp -> {
					com.lms.enrollment.entity.Enrollment e = enrollmentRepository.findById(lp.getEnrollmentId())
							.orElse(null);
					return e != null && e.getStudentId().equals(studentId) && lp.getIsCompleted();
				})
				.toList();
		
		// Award FIRST_LESSON badge if this is the 1st lesson completion
		if (allStudentProgress.size() == 1 && !badgeRepository.existsByStudentIdAndBadgeType(studentId, BadgeType.FIRST_LESSON)) {
			badgeService.awardBadge(studentId, BadgeType.FIRST_LESSON);
		}
		
		// Check if all lessons in current course are completed
		checkAndAwardCourseCompleteBadge(studentId, enrollment);
	}
	
	private void checkAndAwardCourseCompleteBadge(Long studentId, Enrollment enrollment) {
		long totalLessons = lessonRepository.countByCourseId(enrollment.getCourseId());
		long completedLessons = lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(enrollment.getId());

		if (totalLessons > 0 && completedLessons == totalLessons) {
			enrollment.setStatus(EnrollmentStatus.COMPLETED);
			enrollment.setCompletedAt(LocalDateTime.now());
			enrollmentRepository.save(enrollment);
		}
		
		// Award COURSE_COMPLETE badge if all lessons are completed
		if (totalLessons > 0 && completedLessons == totalLessons 
				&& !badgeRepository.existsByStudentIdAndBadgeType(studentId, BadgeType.COURSE_COMPLETE)) {
			badgeService.awardBadge(studentId, BadgeType.COURSE_COMPLETE);
		}
	}

	private void updateLearningStreak(Long studentId) {
		LocalDate today = LocalDate.now();
		LearningStreak streak = streakRepository.findByStudentId(studentId)
				.orElse(LearningStreak.builder().studentId(studentId).build());

		LocalDate lastActive = streak.getLastActiveDate();
		if (lastActive != null) {
			if (lastActive.equals(today)) {
				return;
			}
			if (lastActive.plusDays(1).equals(today)) {
				streak.setCurrentStreak(streak.getCurrentStreak() + 1);
			} else {
				streak.setCurrentStreak(1);
			}
		} else {
			streak.setCurrentStreak(1);
		}

		streak.setLastActiveDate(today);
		if (streak.getCurrentStreak() > streak.getLongestStreak()) {
			streak.setLongestStreak(streak.getCurrentStreak());
		}

		streakRepository.save(streak);
	}
}