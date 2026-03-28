package com.lms.notification.dto.response;

import java.time.LocalDateTime;

import com.lms.notification.vo.NotificationChannel;
import com.lms.notification.vo.NotificationType;

import lombok.Data;

@Data
public class NotificationResponseDto {
	private Long id;
	private Long userId;
	private NotificationType type;
	private NotificationChannel channel;
	private String subject;
	private String body;
	private Boolean isRead;
	private LocalDateTime sentAt;
}