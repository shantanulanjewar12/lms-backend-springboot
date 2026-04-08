package com.lms.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.shared.dto.ApiResponse;
import com.lms.user.dto.request.ForgotPasswordRequestDto;
import com.lms.user.dto.request.LoginRequestDto;
import com.lms.user.dto.request.RegisterRequestDto;
import com.lms.user.dto.request.ResendOtpRequestDto;
import com.lms.user.dto.request.ResetPasswordRequestDto;
import com.lms.user.dto.request.VerifyOtpRequestDto;
import com.lms.user.dto.request.VerifyPasswordResetOtpRequestDto;
import com.lms.user.dto.response.AuthResponseDto;
import com.lms.user.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponseDto>> register(@Valid @RequestBody RegisterRequestDto request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("User registered successfully", authService.register(request)));
	}

	
	
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
		return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
	}
	
	

	@PostMapping("/verify-otp")
	public ResponseEntity<ApiResponse<AuthResponseDto>> verifyOtp(@Valid @RequestBody VerifyOtpRequestDto request) {
		return ResponseEntity.ok(ApiResponse.success("Email verified successfully", authService.verifyOtp(request)));
	}

	
	
	@PostMapping("/resend-otp")
	public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequestDto request) {
		authService.resendOtp(request);
		return ResponseEntity.ok(ApiResponse.success("OTP sent to email"));
	}
	
	

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<Void>> requestForgotPasswordOtp(
			@Valid @RequestBody ForgotPasswordRequestDto request) {
		authService.requestPasswordResetOtp(request);
		return ResponseEntity.ok(ApiResponse.success("If account exists, password reset OTP has been sent"));
	}

	
	
	@PostMapping("/forgot-password/verify-otp")
	public ResponseEntity<ApiResponse<Void>> verifyForgotPasswordOtp(
			@Valid @RequestBody VerifyPasswordResetOtpRequestDto request) {
		authService.verifyPasswordResetOtp(request);
		return ResponseEntity.ok(ApiResponse.success("OTP verified successfully"));
	}

	
	
	@PostMapping("/forgot-password/reset")
	public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequestDto request) {
		authService.resetPassword(request);
		return ResponseEntity.ok(ApiResponse.success("Password reset successful"));
	}
	
	
}