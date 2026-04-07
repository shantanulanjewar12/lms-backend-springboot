package com.lms.course.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lms.course.entity.Course;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
	Page<Course> findByStatus(CourseStatus status, Pageable pageable);

	Page<Course> findByStatusAndCategory(CourseStatus status, String category, Pageable pageable);

	Page<Course> findByStatusAndCategoryAndLevel(CourseStatus status, String category, CourseLevel level, Pageable pageable);

	Page<Course> findByStatusAndLevel(CourseStatus status, CourseLevel level, Pageable pageable);

	Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

	List<Course> findByInstructorId(Long instructorId);

	long countByInstructorId(Long instructorId);

	// Search methods for title, description
	@Query("SELECT c FROM Course c WHERE c.status = :status AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<Course> findByStatusAndSearchTerm(@Param("status") CourseStatus status, @Param("search") String search, Pageable pageable);

	@Query("SELECT c FROM Course c WHERE c.status = :status AND c.category = :category AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<Course> findByStatusAndCategoryAndSearchTerm(@Param("status") CourseStatus status, @Param("category") String category, @Param("search") String search, Pageable pageable);

	@Query("SELECT c FROM Course c WHERE c.status = :status AND c.level = :level AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<Course> findByStatusAndLevelAndSearchTerm(@Param("status") CourseStatus status, @Param("level") CourseLevel level, @Param("search") String search, Pageable pageable);

	@Query("SELECT c FROM Course c WHERE c.status = :status AND c.category = :category AND c.level = :level AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
	Page<Course> findByStatusAndCategoryAndLevelAndSearchTerm(@Param("status") CourseStatus status, @Param("category") String category, @Param("level") CourseLevel level, @Param("search") String search, Pageable pageable);
}