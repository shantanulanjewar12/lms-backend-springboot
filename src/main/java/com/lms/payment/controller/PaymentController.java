package com.lms.payment.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.payment.dto.request.InitiatePaymentRequestDto;
import com.lms.payment.dto.response.PaymentHistoryResponseDto;
import com.lms.payment.dto.response.PaymentOrderResponseDto;
import com.lms.payment.service.PaymentService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/initiate")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<PaymentOrderResponseDto>> initiatePayment(Authentication auth,
			@Valid @RequestBody InitiatePaymentRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment order created", paymentService.initiatePayment(userId, request)));
	}

	@GetMapping("/history")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<List<PaymentHistoryResponseDto>>> getHistory(Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Payment history", paymentService.getPaymentHistory(userId)));
	}

	@GetMapping("/verify/{orderId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<PaymentOrderResponseDto>> verifyPayment(@PathVariable String orderId) {
		return ResponseEntity.ok(ApiResponse.success("Payment status", paymentService.verifyPayment(orderId)));
	}

	@GetMapping("/admin/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<PaymentHistoryResponseDto>>> getAllTransactions(
			@PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("All transactions", paymentService.getAllTransactions(pageable)));
	}
}