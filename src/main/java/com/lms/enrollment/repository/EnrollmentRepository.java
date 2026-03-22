package com.lms.enrollment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.vo.EnrollmentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
	boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

	Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

	List<Enrollment> findByStudentId(Long studentId);

	List<Enrollment> findByCourseId(Long courseId);

	long countByCourseId(Long courseId);

	long countByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
}