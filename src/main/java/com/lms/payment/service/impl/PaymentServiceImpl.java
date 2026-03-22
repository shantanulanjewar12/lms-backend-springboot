package com.lms.payment.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lms.course.entity.Course;
import com.lms.course.service.CourseService;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.notification.service.EmailService;
import com.lms.payment.dto.request.InitiatePaymentRequestDto;
import com.lms.payment.dto.response.PaymentHistoryResponseDto;
import com.lms.payment.dto.response.PaymentOrderResponseDto;
import com.lms.payment.entity.PaymentTransaction;
import com.lms.payment.repository.PaymentTransactionRepository;
import com.lms.payment.service.PaymentService;
import com.lms.payment.vo.PaymentStatus;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentTransactionRepository paymentRepository;
	private final EnrollmentService enrollmentService;
	private final UserService userService;
	private final CourseService courseService;
	private final EmailService emailService;

	@Value("${stripe.publishable-key}")
	private String stripePublishableKey;

	@Value("${stripe.secret-key}")
	private String stripeSecretKey;

	@Value("${stripe.webhook-secret:}")
	private String stripeWebhookSecret;

	@Value("${stripe.frontend-url}")
	private String frontendUrl;

	@Override
	public PaymentOrderResponseDto initiatePayment(Long studentId, InitiatePaymentRequestDto request) {
		try {
			if (!isStripeConfigured()) {
				throw new BadRequestException(
						"Stripe is not configured correctly. Set valid stripe.publishable-key and stripe.secret-key.");
			}

			if (paymentRepository.existsByStudentIdAndCourseIdAndStatus(studentId, request.getCourseId(), PaymentStatus.SUCCESS)) {
				throw new BadRequestException("You are already enrolled in this course.");
			}

			Course course = courseService.getCourseEntityById(request.getCourseId());
			Stripe.apiKey = stripeSecretKey;

			String currency = request.getCurrency() == null || request.getCurrency().isBlank() ? "INR"
					: request.getCurrency().toUpperCase();
			long unitAmount = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
			String checkoutBaseUrl = normalizeFrontendUrl(frontendUrl);

			SessionCreateParams params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
					.setSuccessUrl(checkoutBaseUrl + "/checkout/" + request.getCourseId()
							+ "?payment=success&session_id={CHECKOUT_SESSION_ID}")
					.setCancelUrl(checkoutBaseUrl + "/checkout/" + request.getCourseId() + "?payment=cancel")
					.putMetadata("studentId", String.valueOf(studentId))
					.putMetadata("courseId", String.valueOf(request.getCourseId()))
					.addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L)
							.setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency(currency)
									.setUnitAmount(unitAmount)
									.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
											.setName(course.getTitle()).setDescription("Course purchase").build())
									.build())
							.build())
					.build();

			Session session = Session.create(params);

			PaymentTransaction transaction = PaymentTransaction.builder().studentId(studentId)
					.courseId(request.getCourseId()).amount(request.getAmount()).currency(currency)
					.stripeSessionId(session.getId()).status(PaymentStatus.PENDING).build();
			PaymentTransaction saved = paymentRepository.save(transaction);

			PaymentOrderResponseDto dto = new PaymentOrderResponseDto();
			dto.setOrderId(saved.getId());
			dto.setStripeSessionId(session.getId());
			dto.setAmount(request.getAmount());
			dto.setCurrency(currency);
			dto.setStatus("PENDING");
			dto.setStripePublishableKey(stripePublishableKey);
			dto.setCheckoutUrl(session.getUrl());
			return dto;

		} catch (StripeException e) {
			throw new BadRequestException("Failed to create Stripe checkout session: " + e.getMessage());
		}
	}

	@Override
	public void handleWebhook(String payload, String signature) {
		try {
			if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
				throw new BadRequestException("Stripe webhook secret is not configured.");
			}

			Stripe.apiKey = stripeSecretKey;
			Event event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);

			if (!("checkout.session.completed".equals(event.getType())
					|| "checkout.session.async_payment_succeeded".equals(event.getType())
					|| "checkout.session.expired".equals(event.getType())
					|| "checkout.session.async_payment_failed".equals(event.getType()))) {
				return;
			}

			EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
			Session session = (Session) dataObjectDeserializer.getObject()
					.orElseThrow(() -> new BadRequestException("Unable to deserialize Stripe webhook payload."));

			PaymentTransaction transaction = paymentRepository.findByStripeSessionId(session.getId())
					.orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
			transaction.setStripeSignature(signature);

			if ("checkout.session.completed".equals(event.getType())
					|| "checkout.session.async_payment_succeeded".equals(event.getType())) {
				markPaymentAsSuccess(transaction, session.getPaymentIntent());
			} else if (transaction.getStatus() == PaymentStatus.PENDING) {
				transaction.setStatus(PaymentStatus.FAILED);
				paymentRepository.save(transaction);
			}
		} catch (SignatureVerificationException e) {
			throw new BadRequestException("Invalid Stripe webhook signature");
		} catch (Exception e) {
			log.error("Webhook processing error: {}", e.getMessage());
			throw new BadRequestException("Webhook processing failed: " + e.getMessage());
		}
	}

	@Override
	public List<PaymentHistoryResponseDto> getPaymentHistory(Long studentId) {
		return paymentRepository.findByStudentId(studentId).stream().map(t -> toHistoryDto(t, false)).toList();
	}

	@Override
	public PaymentOrderResponseDto verifyPayment(String orderId) {
		PaymentTransaction t = paymentRepository.findByStripeSessionId(orderId)
				.orElseThrow(() -> new ResourceNotFoundException("Session not found: " + orderId));

		try {
			Stripe.apiKey = stripeSecretKey;
			Session session = Session.retrieve(orderId);
			if (("paid".equalsIgnoreCase(session.getPaymentStatus()) || "complete".equalsIgnoreCase(session.getStatus()))
					&& t.getStatus() != PaymentStatus.SUCCESS) {
				markPaymentAsSuccess(t, session.getPaymentIntent());
			}
		} catch (StripeException e) {
			log.warn("Stripe verification failed for session {}: {}", orderId, e.getMessage());
		}

		PaymentOrderResponseDto dto = new PaymentOrderResponseDto();
		dto.setOrderId(t.getId());
		dto.setStripeSessionId(t.getStripeSessionId());
		dto.setAmount(t.getAmount());
		dto.setCurrency(t.getCurrency());
		dto.setStatus(t.getStatus().name());
		dto.setStripePublishableKey(stripePublishableKey);
		return dto;
	}

	@Override
	public Page<PaymentHistoryResponseDto> getAllTransactions(Pageable pageable) {
		return paymentRepository.findAll(pageable).map(t -> toHistoryDto(t, true));
	}

	private PaymentHistoryResponseDto toHistoryDto(PaymentTransaction transaction, boolean includeStudentName) {
		PaymentHistoryResponseDto dto = new PaymentHistoryResponseDto();
		dto.setId(transaction.getId());
		dto.setStudentId(transaction.getStudentId());
		dto.setCourseId(transaction.getCourseId());
		dto.setAmount(transaction.getAmount());
		dto.setCurrency(transaction.getCurrency());
		dto.setStatus(transaction.getStatus());
		dto.setStripeSessionId(transaction.getStripeSessionId());
		dto.setCreatedAt(transaction.getCreatedAt());

		if (includeStudentName) {
			try {
				UserResponseDto student = userService.getUserById(transaction.getStudentId());
				dto.setStudentName(student.getFullName());
			} catch (Exception e) {
				log.debug("Could not resolve student name for payment {}", transaction.getId());
			}
		}

		try {
			Course course = courseService.getCourseEntityById(transaction.getCourseId());
			dto.setCourseTitle(course.getTitle());
		} catch (Exception e) {
			log.debug("Could not resolve course title for payment {}", transaction.getId());
		}

		return dto;
	}

	private void markPaymentAsSuccess(PaymentTransaction transaction, String paymentIntentId) {
		if (transaction.getStatus() == PaymentStatus.SUCCESS) {
			return;
		}

		transaction.setStripePaymentIntentId(paymentIntentId);
		transaction.setStatus(PaymentStatus.SUCCESS);
		paymentRepository.save(transaction);

		try {
			enrollmentService.createEnrollmentDirectly(transaction.getStudentId(), transaction.getCourseId());
		} catch (Exception e) {
			log.warn("Enrollment creation skipped for transaction {}: {}", transaction.getId(), e.getMessage());
		}

		try {
			UserResponseDto student = userService.getUserById(transaction.getStudentId());
			Course course = courseService.getCourseEntityById(transaction.getCourseId());
			emailService.sendPaymentSuccessEmail(student.getEmail(), student.getFullName(), course.getTitle(),
					transaction.getAmount(), transaction.getCurrency(), paymentIntentId);
		} catch (Exception e) {
			log.warn("Payment success email skipped for transaction {}: {}", transaction.getId(), e.getMessage());
		}
	}

	private boolean isStripeConfigured() {
		return stripePublishableKey != null && !stripePublishableKey.isBlank() && stripeSecretKey != null
				&& !stripeSecretKey.isBlank();
	}

	private String normalizeFrontendUrl(String rawUrl) {
		if (rawUrl == null || rawUrl.isBlank()) {
			throw new BadRequestException("stripe.frontend-url is not configured.");
		}

		String url = rawUrl.trim();
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			url = "http://" + url;
		}

		while (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		return url;
	}
}