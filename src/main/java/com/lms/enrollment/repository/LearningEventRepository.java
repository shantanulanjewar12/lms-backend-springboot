package com.lms.enrollment.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.vo.LearningEventType;

@Repository
public interface LearningEventRepository extends JpaRepository<LearningEvent, Long> {
	List<LearningEvent> findByLessonId(Long lessonId);

	void deleteByLessonId(Long lessonId);

	List<LearningEvent> findByStudentIdAndEventType(Long studentId, LearningEventType eventType);

	List<LearningEvent> findByStudentIdAndEventTypeInAndCreatedAtBetween(Long studentId,
		List<LearningEventType> eventTypes, LocalDateTime from, LocalDateTime to);

	List<LearningEvent> findByStudentIdAndCreatedAtBetween(Long studentId, LocalDateTime from,
		LocalDateTime to);

	long countByLessonIdAndEventType(Long lessonId, LearningEventType eventType);

	long countByLessonIdAndEventTypeAndValueGreaterThanEqual(Long lessonId, LearningEventType eventType,
			java.math.BigDecimal value);
}