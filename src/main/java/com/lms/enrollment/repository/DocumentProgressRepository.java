package com.lms.enrollment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.enrollment.entity.DocumentProgress;

@Repository
public interface DocumentProgressRepository extends JpaRepository<DocumentProgress, Long> {
	List<DocumentProgress> findByEnrollmentId(Long enrollmentId);

	Optional<DocumentProgress> findByEnrollmentIdAndLessonId(Long enrollmentId, Long lessonId);

	long countByEnrollmentIdAndIsCompletedTrue(Long enrollmentId);

	void deleteByLessonId(Long lessonId);
}
