package com.lms.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.notification.entity.LearningStreak;

public interface LearningStreakRepository extends JpaRepository<LearningStreak, Long> {
	Optional<LearningStreak> findByStudentId(Long studentId);
}