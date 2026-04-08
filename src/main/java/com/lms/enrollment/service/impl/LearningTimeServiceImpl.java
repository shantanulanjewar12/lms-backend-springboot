package com.lms.enrollment.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.service.LearningTimeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningTimeServiceImpl implements LearningTimeService {

	private final EnrollmentRepository enrollmentRepository;

	@Override
	@Transactional
	public void addLearningTime(Enrollment enrollment, long durationSeconds) {
		if (enrollment == null || durationSeconds <= 0) {
			return;
		}

		Long currentTime = enrollment.getTotalTimeSpentSeconds() != null ? enrollment.getTotalTimeSpentSeconds() : 0L;
		enrollment.setTotalTimeSpentSeconds(currentTime + durationSeconds);
		enrollment.setLastAccessedAt(LocalDateTime.now());
		enrollmentRepository.save(enrollment);
		log.debug("Added {} seconds to enrollment {}, total: {} seconds", durationSeconds, enrollment.getId(),
				enrollment.getTotalTimeSpentSeconds());
	}

	@Override
	public String formatLearningTime(Long totalSeconds) {
		if (totalSeconds == null || totalSeconds <= 0) {
			return "0m";
		}

		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;

		if (hours > 0) {
			return String.format("%dh %dm", hours, minutes);
		} else {
			return String.format("%dm", minutes);
		}
	}

	@Override
	public Long getTotalLearningTime(Long enrollmentId) {
		return enrollmentRepository.findById(enrollmentId).map(Enrollment::getTotalTimeSpentSeconds).orElse(0L);
	}

	@Override
	@Transactional
	public void updateLastAccessedTime(Enrollment enrollment) {
		if (enrollment != null) {
			enrollment.setLastAccessedAt(LocalDateTime.now());
			enrollmentRepository.save(enrollment);
		}
	}
}
