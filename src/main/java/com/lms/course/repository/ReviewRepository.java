package com.lms.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.lms.course.entity.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
	List<Review> findByCourseId(Long courseId);

	boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

	Optional<Review> findByCourseIdAndStudentId(Long courseId, Long studentId);

	@Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
	Double findAverageRatingByCourseId(Long courseId);

	long countByCourseId(Long courseId);
}