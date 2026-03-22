package com.lms.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.course.entity.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {
	List<Lesson> findByCourseIdOrderByOrderIndex(Long courseId);

	long countByCourseId(Long courseId);
}