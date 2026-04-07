package com.lms.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.course.entity.QuizAttempt;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
	List<QuizAttempt> findByStudentIdAndQuizIdOrderBySubmittedAtDesc(Long studentId, Long quizId);

	Optional<QuizAttempt> findTopByStudentIdAndQuizIdOrderBySubmittedAtDesc(Long studentId, Long quizId);
}
