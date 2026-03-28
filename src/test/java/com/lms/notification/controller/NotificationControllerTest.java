package com.lms.notification.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.lms.notification.dto.response.NotificationResponseDto;
import com.lms.notification.service.NotificationService;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    private NotificationResponseDto notificationResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();

        notificationResponseDto = new NotificationResponseDto();
        notificationResponseDto.setId(1L);
        notificationResponseDto.setUserId(10L);
        notificationResponseDto.setSubject("Course updated");
        notificationResponseDto.setBody("Your course was updated");
        notificationResponseDto.setIsRead(false);
    }

    @Test
    void getMyNotifications_shouldReturnList() throws Exception {
        when(notificationService.getMyNotifications(10L))
                .thenReturn(List.of(notificationResponseDto));

        mockMvc.perform(get("/api/v1/notifications/my")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notifications fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(10))
                .andExpect(jsonPath("$.data[0].subject").value("Course updated"))
                .andExpect(jsonPath("$.data[0].body").value("Your course was updated"))
                .andExpect(jsonPath("$.data[0].isRead").value(false));
    }

    @Test
    void markAsRead_shouldReturnSuccess() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/api/v1/notifications/1/read")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Marked as read"));
    }

    @Test
    void getMyNotifications_shouldThrowException_whenAuthenticationIsNull() {
        assertThrows(NullPointerException.class,
                () -> notificationController.getMyNotifications(null));
    }

    private Authentication createAuth(Long userId, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return userId;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "user";
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return "user";
            }
        };
    }
}
