package com.lms.notification.service;

import java.util.List;

import com.lms.notification.dto.response.NotificationResponseDto;
import com.lms.notification.vo.NotificationChannel;
import com.lms.notification.vo.NotificationType;

public interface NotificationService {
	void sendNotification(Long userId, NotificationType type, NotificationChannel channel, String subject, String body);

	List<NotificationResponseDto> getMyNotifications(Long userId);

	void markAsRead(Long notificationId);
}