package com.lms.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.user.dto.request.ForgotPasswordRequestDto;
import com.lms.user.dto.request.LoginRequestDto;
import com.lms.user.dto.request.RegisterRequestDto;
import com.lms.user.dto.request.ResendOtpRequestDto;
import com.lms.user.dto.request.ResetPasswordRequestDto;
import com.lms.user.dto.request.VerifyOtpRequestDto;
import com.lms.user.dto.request.VerifyPasswordResetOtpRequestDto;
import com.lms.user.dto.response.AuthResponseDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.AuthService;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.lms\\.shared\\.jwt\\..*"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.lms\\.shared\\.security\\..*")
    }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private AuthResponseDto buildAuthResponse() {
        UserResponseDto user = new UserResponseDto();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("test@gmail.com");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);

        AuthResponseDto response = new AuthResponseDto();
        response.setAccessToken("token123");
        response.setUser(user);
        return response;
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201")
    void register_shouldReturnCreated() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("Test User");
        request.setEmail("test@gmail.com");
        request.setPassword("123456");
        request.setRole(UserRole.STUDENT);

        when(authService.register(any(RegisterRequestDto.class)))
                .thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("token123"))
                .andExpect(jsonPath("$.data.user.email").value("test@gmail.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200")
    void login_shouldReturnOk() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(authService.login(any(LoginRequestDto.class)))
                .thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("token123"))
                .andExpect(jsonPath("$.data.user.fullName").value("Test User"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp should return 200")
    void verifyOtp_shouldReturnOk() throws Exception {
        VerifyOtpRequestDto request = new VerifyOtpRequestDto();
        request.setEmail("test@gmail.com");
        request.setOtp("123456");

        when(authService.verifyOtp(any(VerifyOtpRequestDto.class)))
                .thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.data.user.email").value("test@gmail.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/resend-otp should return 200")
    void resendOtp_shouldReturnOk() throws Exception {
        ResendOtpRequestDto request = new ResendOtpRequestDto();
        request.setEmail("test@gmail.com");

        doNothing().when(authService).resendOtp(any(ResendOtpRequestDto.class));

        mockMvc.perform(post("/api/v1/auth/resend-otp")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP sent to email"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password should return 200")
    void forgotPassword_shouldReturnOk() throws Exception {
        ForgotPasswordRequestDto request = new ForgotPasswordRequestDto();
        request.setEmail("test@gmail.com");

        doNothing().when(authService).requestPasswordResetOtp(any(ForgotPasswordRequestDto.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("If account exists, password reset OTP has been sent"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password/verify-otp should return 200")
    void verifyForgotPasswordOtp_shouldReturnOk() throws Exception {
        VerifyPasswordResetOtpRequestDto request = new VerifyPasswordResetOtpRequestDto();
        request.setEmail("test@gmail.com");
        request.setOtp("123456");

        doNothing().when(authService).verifyPasswordResetOtp(any(VerifyPasswordResetOtpRequestDto.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password/verify-otp")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OTP verified successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password/reset should return 200")
    void resetPassword_shouldReturnOk() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("test@gmail.com");
        request.setOtp("123456");
        request.setNewPassword("newpass123");
        request.setConfirmPassword("newpass123");

        doNothing().when(authService).resetPassword(any(ResetPasswordRequestDto.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password/reset")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password reset successful"));
    }
}
