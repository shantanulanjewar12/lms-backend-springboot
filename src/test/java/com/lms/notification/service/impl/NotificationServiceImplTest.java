package com.lms.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.notification.dto.response.NotificationResponseDto;
import com.lms.notification.entity.Notification;
import com.lms.notification.repository.NotificationRepository;
import com.lms.notification.vo.NotificationChannel;
import com.lms.notification.vo.NotificationType;
import com.lms.shared.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;
    private NotificationResponseDto notificationResponseDto;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .id(1L)
                .userId(10L)
                .type(NotificationType.COURSE_UPDATE) // ✅ FIXED
                .channel(NotificationChannel.EMAIL)
                .subject("Course updated")
                .body("Your course was updated successfully")
                .isRead(false)
                .build();

        notificationResponseDto = new NotificationResponseDto();
        notificationResponseDto.setId(1L);
        notificationResponseDto.setUserId(10L);
        notificationResponseDto.setType(NotificationType.COURSE_UPDATE); // ✅ FIXED
        notificationResponseDto.setChannel(NotificationChannel.EMAIL);
        notificationResponseDto.setSubject("Course updated");
        notificationResponseDto.setBody("Your course was updated successfully");
        notificationResponseDto.setIsRead(false);
    }

    @Test
    void sendNotification_shouldSaveNotification() {
        notificationService.sendNotification(
                10L,
                NotificationType.COURSE_UPDATE, // ✅ FIXED
                NotificationChannel.EMAIL,
                "Course updated",
                "Your course was updated successfully"
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertEquals(10L, saved.getUserId());
        assertEquals(NotificationType.COURSE_UPDATE, saved.getType());
        assertEquals(NotificationChannel.EMAIL, saved.getChannel());
        assertEquals("Course updated", saved.getSubject());
        assertEquals("Your course was updated successfully", saved.getBody());
    }

    @Test
    void getMyNotifications_shouldReturnMappedNotifications() {
        when(notificationRepository.findByUserIdOrderBySentAtDesc(10L))
                .thenReturn(List.of(notification));

        when(modelMapper.map(notification, NotificationResponseDto.class))
                .thenReturn(notificationResponseDto);

        List<NotificationResponseDto> result = notificationService.getMyNotifications(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getUserId());
        assertEquals(NotificationType.COURSE_UPDATE, result.get(0).getType());
        assertEquals(NotificationChannel.EMAIL, result.get(0).getChannel());
        assertEquals("Course updated", result.get(0).getSubject());
        assertEquals("Your course was updated successfully", result.get(0).getBody());
        assertEquals(false, result.get(0).getIsRead());
    }

    @Test
    void getMyNotifications_shouldReturnEmptyList_whenNoNotifications() {
        when(notificationRepository.findByUserIdOrderBySentAtDesc(10L))
                .thenReturn(List.of());

        List<NotificationResponseDto> result = notificationService.getMyNotifications(10L);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void markAsRead_shouldUpdateNotificationToRead() {
        when(notificationRepository.findById(1L))
                .thenReturn(Optional.of(notification));

        notificationService.markAsRead(1L);

        assertEquals(true, notification.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldThrowException_whenNotFound() {
        when(notificationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(1L));

        verify(notificationRepository, never()).save(any(Notification.class));
    }
}