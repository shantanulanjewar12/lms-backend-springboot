package com.lms.enrollment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.enrollment.entity.LessonProgress;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {
	List<LessonProgress> findByEnrollmentId(Long enrollmentId);

	void deleteByLessonId(Long lessonId);

	Optional<LessonProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

	long countByEnrollmentIdAndIsCompletedTrue(Long enrollmentId);

	long countByLessonIdAndIsCompletedTrue(Long lessonId);
}