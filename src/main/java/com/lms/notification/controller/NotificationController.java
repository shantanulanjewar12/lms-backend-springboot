package com.lms.notification.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.notification.dto.response.NotificationResponseDto;
import com.lms.notification.service.NotificationService;
import com.lms.shared.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping("/my")
	@PreAuthorize("hasAnyRole('STUDENT','INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<List<NotificationResponseDto>>> getMyNotifications(Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Notifications fetched", notificationService.getMyNotifications(userId)));
	}

	@PutMapping("/{id}/read")
	@PreAuthorize("hasAnyRole('STUDENT','INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.ok(ApiResponse.success("Marked as read"));
	}
}