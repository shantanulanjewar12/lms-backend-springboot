package com.lms.enrollment.entity;

import java.time.LocalDateTime;

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
@Table(name = "lesson_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "enrollment_id", nullable = false)
	private Long enrollmentId;

	@Column(name = "lesson_id", nullable = false)
	private Long lessonId;

	@Builder.Default
	@Column(name = "is_completed")
	private Boolean isCompleted = false;

	@Builder.Default
	@Column(name = "watch_duration_seconds")
	private Integer watchDurationSeconds = 0;

	@Column(name = "last_watched_at")
	private LocalDateTime lastWatchedAt;
}