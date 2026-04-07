package com.lms.course.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "quiz_attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "course_id", nullable = false)
	private Long courseId;

	@Column(name = "quiz_id", nullable = false)
	private Long quizId;

	@Column(name = "answers_json", columnDefinition = "TEXT", nullable = false)
	private String answersJson;

	@Column(name = "score_percent")
	private Double scorePercent;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private LocalDateTime submittedAt;
}
