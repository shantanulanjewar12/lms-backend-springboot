package com.lms.course.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lms.course.vo.ContentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "lessons")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id", nullable = false)
	@ToString.Exclude
	private Course course;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(name = "content_url")
	private String contentUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "content_type", length = 20)
	private ContentType contentType;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex;

	@Builder.Default
	@Column(name = "is_free_preview")
	private Boolean isFreePreview = false;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;
}