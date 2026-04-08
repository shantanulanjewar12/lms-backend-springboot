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
@Table(name = "document_progress")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProgress {

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
	@Column(name = "reading_time_seconds")
	private Integer readingTimeSeconds = 0;

	@Builder.Default
	@Column(name = "read_count")
	private Integer readCount = 0;

	@Column(name = "last_read_at")
	private LocalDateTime lastReadAt;
}
