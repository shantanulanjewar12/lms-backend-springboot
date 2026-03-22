package com.lms.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.course.entity.QuizQuestion;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {

	List<QuizQuestion> findByLessonIdOrderByOrderIndex(Long lessonId);

	void deleteByLessonId(Long lessonId);
}
