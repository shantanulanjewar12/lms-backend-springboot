package com.lms.course.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.course.entity.Course;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
	Page<Course> findByStatus(CourseStatus status, Pageable pageable);

	Page<Course> findByStatusAndCategory(CourseStatus status, String category, Pageable pageable);

	Page<Course> findByStatusAndLevel(CourseStatus status, CourseLevel level, Pageable pageable);

	Page<Course> findByInstructorId(Long instructorId, Pageable pageable);

	List<Course> findByInstructorId(Long instructorId);

	long countByInstructorId(Long instructorId);
}