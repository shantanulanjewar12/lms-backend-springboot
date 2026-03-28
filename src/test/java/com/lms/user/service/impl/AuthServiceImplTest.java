package com.lms.user.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lms.course.service.S3StorageService;
import com.lms.notification.service.EmailService;
import com.lms.shared.jwt.JwtUtil;
import com.lms.user.dto.request.LoginRequestDto;
import com.lms.user.dto.request.RegisterRequestDto;
import com.lms.user.dto.request.VerifyOtpRequestDto;
import com.lms.user.dto.response.AuthResponseDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private EmailService emailService;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setup() {
        user = User.builder()
                .id(1L)
                .fullName("Test User")
                .email("test@gmail.com")
                .passwordHash("encodedPassword")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();

        userResponseDto = new UserResponseDto();
        userResponseDto.setId(1L);
        userResponseDto.setFullName("Test User");
        userResponseDto.setEmail("test@gmail.com");
        userResponseDto.setRole(UserRole.STUDENT);
        userResponseDto.setStatus(UserStatus.ACTIVE);
    }

    // ================= REGISTER =================

    @Test
    void register_shouldThrowException_ifEmailExists() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@gmail.com");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> authService.register(request));
    }

    @Test
    void register_shouldSaveUser_andSendOtp() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("Test User");
        request.setEmail("test@gmail.com");
        request.setPassword("123456");
        request.setRole(UserRole.STUDENT);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // 👉 Only here
        when(modelMapper.map(any(User.class), eq(UserResponseDto.class))).thenReturn(userResponseDto);
        when(s3StorageService.getAccessibleFileUrl(any())).thenReturn(null);

        AuthResponseDto response = authService.register(request);

        assertNotNull(response);
        verify(emailService).sendRegistrationOtpEmail(anyString(), anyString(), anyString());
    }

    // ================= LOGIN =================

    @Test
    void login_shouldReturnToken_whenValidCredentials() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(anyLong(), anyString(), anyString())).thenReturn("token");

        // 👉 Only here
        when(modelMapper.map(any(User.class), eq(UserResponseDto.class))).thenReturn(userResponseDto);
        when(s3StorageService.getAccessibleFileUrl(any())).thenReturn(null);

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("token", response.getAccessToken());
    }

    @Test
    void login_shouldThrowException_whenWrongPassword() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("wrongPassword");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class,
                () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenUserNotVerified() {
        user.setEmailVerified(false);

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> authService.login(request));
    }

    @Test
    void login_shouldThrowException_whenUserInactive() {
        user.setStatus(UserStatus.SUSPENDED);

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@gmail.com");
        request.setPassword("123456");

        when(userRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "encodedPassword")).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> authService.login(request));
    }

    // ================= VERIFY OTP =================

    @Test
    void verifyOtp_shouldThrowException_ifUserNotFound() {
        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.empty());

        VerifyOtpRequestDto request = new VerifyOtpRequestDto();
        request.setEmail("test@gmail.com");
        request.setOtp("123456");

        assertThrows(RuntimeException.class,
                () -> authService.verifyOtp(request));
    }
}