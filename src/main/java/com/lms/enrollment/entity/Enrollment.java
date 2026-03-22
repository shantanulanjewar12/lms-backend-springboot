package com.lms.enrollment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lms.enrollment.vo.EnrollmentStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "enrollments", uniqueConstraints = @UniqueConstraint(columnNames = { "student_id", "course_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_id", nullable = false)
	private Long studentId;

	@Column(name = "course_id", nullable = false)
	private Long courseId;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

	@CreationTimestamp
	@Column(name = "enrolled_at", updatable = false)
	private LocalDateTime enrolledAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;
}