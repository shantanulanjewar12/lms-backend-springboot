package com.lms.user.service.impl;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lms.notification.service.EmailService;
import com.lms.course.service.S3StorageService;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.jwt.JwtUtil;
import com.lms.user.dto.request.ForgotPasswordRequestDto;
import com.lms.user.dto.request.LoginRequestDto;
import com.lms.user.dto.request.RegisterRequestDto;
import com.lms.user.dto.request.ResendOtpRequestDto;
import com.lms.user.dto.request.ResetPasswordRequestDto;
import com.lms.user.dto.request.VerifyOtpRequestDto;
import com.lms.user.dto.request.VerifyPasswordResetOtpRequestDto;
import com.lms.user.dto.response.AuthResponseDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.service.AuthService;
import com.lms.user.vo.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final ModelMapper modelMapper;
	private final EmailService emailService;
	private final S3StorageService s3StorageService;

	private static final long OTP_EXPIRY_MINUTES = 10;

	@Override
	public AuthResponseDto register(RegisterRequestDto request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new BadRequestException("Email already registered: " + request.getEmail());
		}
		String otp = generateOtp();
		LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

		User user = User.builder().fullName(request.getFullName()).email(request.getEmail())
				.passwordHash(passwordEncoder.encode(request.getPassword())).role(request.getRole()).emailVerified(false)
				.emailOtp(otp).emailOtpExpiresAt(expiry).build();
		User saved = userRepository.save(user);

		emailService.sendRegistrationOtpEmail(saved.getEmail(), saved.getFullName(), otp);

		return AuthResponseDto.builder().accessToken(null).user(toUserResponseDto(saved)).build();
	}

	@Override
	public AuthResponseDto verifyOtp(VerifyOtpRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("User not found with this email."));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BadRequestException("Your account is not active. Please contact support.");
		}

		if (Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new BadRequestException("Email is already verified");
		}
		if (user.getEmailOtp() == null || user.getEmailOtpExpiresAt() == null) {
			throw new BadRequestException("OTP not generated. Please request a new OTP.");
		}
		if (LocalDateTime.now().isAfter(user.getEmailOtpExpiresAt())) {
			throw new BadRequestException("OTP has expired. Please request a new OTP.");
		}
		if (!request.getOtp().equals(user.getEmailOtp())) {
			throw new BadRequestException("Invalid OTP");
		}

		user.setEmailVerified(true);
		user.setEmailOtp(null);
		user.setEmailOtpExpiresAt(null);
		User saved = userRepository.save(user);

		emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());

		String token = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());
		return AuthResponseDto.builder().accessToken(token).user(toUserResponseDto(saved)).build();
	}

	@Override
	public void resendOtp(ResendOtpRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("User not found with this email"));

		if (Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new BadRequestException("Email is already verified");
		}

		String otp = generateOtp();
		user.setEmailOtp(otp);
		user.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
		userRepository.save(user);

		emailService.sendRegistrationOtpEmail(user.getEmail(), user.getFullName(), otp);
	}

	@Override
	public AuthResponseDto login(LoginRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("Invalid email or password"));
		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Invalid email or password");
		}
		if (!Boolean.TRUE.equals(user.getEmailVerified())) {
			throw new BadRequestException("Please verify your email before logging in");
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BadRequestException("Your account is suspended or inactive. Please contact support.");
		}
		String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
		return AuthResponseDto.builder().accessToken(token).user(toUserResponseDto(user)).build();
	}

	private UserResponseDto toUserResponseDto(User user) {
		UserResponseDto dto = modelMapper.map(user, UserResponseDto.class);
		dto.setProfilePic(s3StorageService.getAccessibleFileUrl(dto.getProfilePic()));
		return dto;
	}

	@Override
	public void requestPasswordResetOtp(ForgotPasswordRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail()).orElse(null);
		if (user == null) {
			return;
		}

		if (user.getStatus() != UserStatus.ACTIVE || !Boolean.TRUE.equals(user.getEmailVerified())) {
			return;
		}

		String otp = generateOtp();
		user.setPasswordResetOtp(otp);
		user.setPasswordResetOtpExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
		user.setPasswordResetOtpVerified(false);
		userRepository.save(user);

		emailService.sendPasswordResetOtpEmail(user.getEmail(), user.getFullName(), otp);
	}

	@Override
	public void verifyPasswordResetOtp(VerifyPasswordResetOtpRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("Invalid email or OTP"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BadRequestException("Your account is not active. Please contact support.");
		}
		if (user.getPasswordResetOtp() == null || user.getPasswordResetOtpExpiresAt() == null) {
			throw new BadRequestException("Reset OTP not generated. Please request a new OTP.");
		}
		if (LocalDateTime.now().isAfter(user.getPasswordResetOtpExpiresAt())) {
			throw new BadRequestException("Reset OTP has expired. Please request a new OTP.");
		}
		if (!request.getOtp().equals(user.getPasswordResetOtp())) {
			throw new BadRequestException("Invalid OTP");
		}

		user.setPasswordResetOtpVerified(true);
		userRepository.save(user);
	}

	@Override
	public void resetPassword(ResetPasswordRequestDto request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new BadRequestException("Invalid reset request"));

		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BadRequestException("Your account is not active. Please contact support.");
		}
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw new BadRequestException("New password and confirm password do not match");
		}
		if (user.getPasswordResetOtp() == null || user.getPasswordResetOtpExpiresAt() == null) {
			throw new BadRequestException("Reset OTP not generated. Please request a new OTP.");
		}
		if (LocalDateTime.now().isAfter(user.getPasswordResetOtpExpiresAt())) {
			throw new BadRequestException("Reset OTP has expired. Please request a new OTP.");
		}
		if (!request.getOtp().equals(user.getPasswordResetOtp())) {
			throw new BadRequestException("Invalid OTP");
		}
		if (!Boolean.TRUE.equals(user.getPasswordResetOtpVerified())) {
			throw new BadRequestException("Please verify reset OTP before changing password");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
		user.setPasswordResetOtp(null);
		user.setPasswordResetOtpExpiresAt(null);
		user.setPasswordResetOtpVerified(false);
		userRepository.save(user);
	}

	private String generateOtp() {
		int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);
		return String.valueOf(otp);
	}
}