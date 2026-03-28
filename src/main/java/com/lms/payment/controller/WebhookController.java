package com.lms.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.payment.service.PaymentService;
import com.lms.shared.dto.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class WebhookController {

	private final PaymentService paymentService;

	@PostMapping("/webhook")
	public ResponseEntity<ApiResponse<String>> handleWebhook(@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String signature)
	{
		paymentService.handleWebhook(payload, signature);
		return ResponseEntity.ok(ApiResponse.success("Webhook processed", "OK"));
	}
}