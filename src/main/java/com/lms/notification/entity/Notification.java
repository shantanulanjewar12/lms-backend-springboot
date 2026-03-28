package com.lms.notification.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lms.notification.vo.NotificationChannel;
import com.lms.notification.vo.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private NotificationType type;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private NotificationChannel channel;

	@Column(length = 255)
	private String subject;

	@Column(columnDefinition = "TEXT")
	private String body;

	@Builder.Default
	@Column(name = "is_read")
	private Boolean isRead = false;

	@CreationTimestamp
	@Column(name = "sent_at", updatable = false)
	private LocalDateTime sentAt;
}