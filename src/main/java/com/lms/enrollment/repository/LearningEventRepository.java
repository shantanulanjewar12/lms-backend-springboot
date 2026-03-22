package com.lms.enrollment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.vo.LearningEventType;

@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
	List<LearningEvent> findByLessonId(Long lessonId);

	List<LearningEvent> findByStudentIdAndEventType(Long studentId, LearningEventType eventType);

	long countByLessonIdAndEventType(Long lessonId, LearningEventType eventType);

	long countByLessonIdAndEventTypeAndValueGreaterThanEqual(Long lessonId, LearningEventType eventType,
			java.math.BigDecimal value);
}