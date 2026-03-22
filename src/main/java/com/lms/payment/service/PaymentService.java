package com.lms.payment.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lms.payment.dto.request.InitiatePaymentRequestDto;
import com.lms.payment.dto.response.PaymentHistoryResponseDto;
import com.lms.payment.dto.response.PaymentOrderResponseDto;

public interface PaymentService {
	PaymentOrderResponseDto initiatePayment(Long studentId, InitiatePaymentRequestDto request);

	void handleWebhook(String payload, String signature);

	List<PaymentHistoryResponseDto> getPaymentHistory(Long studentId);

	PaymentOrderResponseDto verifyPayment(String orderId);

	Page<PaymentHistoryResponseDto> getAllTransactions(Pageable pageable);
}