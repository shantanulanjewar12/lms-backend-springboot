package com.lms.user.service;

import com.lms.user.dto.request.LoginRequestDto;
import com.lms.user.dto.request.ForgotPasswordRequestDto;
import com.lms.user.dto.request.RegisterRequestDto;
import com.lms.user.dto.request.ResendOtpRequestDto;
import com.lms.user.dto.request.ResetPasswordRequestDto;
import com.lms.user.dto.request.VerifyOtpRequestDto;
import com.lms.user.dto.request.VerifyPasswordResetOtpRequestDto;
import com.lms.user.dto.response.AuthResponseDto;

public interface AuthService {
	AuthResponseDto register(RegisterRequestDto request);

	AuthResponseDto verifyOtp(VerifyOtpRequestDto request);

	void resendOtp(ResendOtpRequestDto request);

	AuthResponseDto login(LoginRequestDto request);

	void requestPasswordResetOtp(ForgotPasswordRequestDto request);

	void verifyPasswordResetOtp(VerifyPasswordResetOtpRequestDto request);

	void resetPassword(ResetPasswordRequestDto request);
}