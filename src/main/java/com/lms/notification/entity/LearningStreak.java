package com.lms.notification.entity;

import java.time.LocalDate;

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
@Table(name = "learning_streaks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningStreak {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_id", nullable = false, unique = true)
	private Long studentId;

	@Builder.Default
	@Column(name = "current_streak")
	private Integer currentStreak = 0;

	@Builder.Default
	@Column(name = "longest_streak")
	private Integer longestStreak = 0;

	@Column(name = "last_active_date")
	private LocalDate lastActiveDate;
}