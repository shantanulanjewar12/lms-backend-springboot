package com.lms.payment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.lms.course.entity.Course;
import com.lms.course.service.CourseService;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.notification.service.EmailService;
import com.lms.payment.dto.request.InitiatePaymentRequestDto;
import com.lms.payment.dto.response.PaymentHistoryResponseDto;
import com.lms.payment.dto.response.PaymentOrderResponseDto;
import com.lms.payment.entity.PaymentTransaction;
import com.lms.payment.repository.PaymentTransactionRepository;
import com.lms.payment.vo.PaymentStatus;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.service.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private UserService userService;

    @Mock
    private CourseService courseService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Course course;
    private PaymentTransaction transaction;

    @BeforeEach
    void setup() {
        course = new Course();
        course.setId(101L);
        course.setTitle("Java Course");

        transaction = PaymentTransaction.builder()
                .id(1L)
                .studentId(10L)
                .courseId(101L)
                .amount(BigDecimal.valueOf(500))
                .currency("INR")
                .stripeSessionId("sess_123")
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    void initiatePayment_shouldThrowException_whenAlreadyPurchased() {
        InitiatePaymentRequestDto request = new InitiatePaymentRequestDto();
        request.setCourseId(101L);
        request.setAmount(BigDecimal.valueOf(500));

        when(paymentRepository.existsByStudentIdAndCourseIdAndStatus(
                10L, 101L, PaymentStatus.SUCCESS)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> paymentService.initiatePayment(10L, request));
    }

    @Test
    void initiatePayment_shouldThrowException_whenStripeNotConfigured() {
        InitiatePaymentRequestDto request = new InitiatePaymentRequestDto();
        request.setCourseId(101L);
        request.setAmount(BigDecimal.valueOf(500));

        assertThrows(BadRequestException.class,
                () -> paymentService.initiatePayment(10L, request));
    }

    @Test
    void getPaymentHistory_shouldReturnList() {
        when(paymentRepository.findByStudentId(10L))
                .thenReturn(List.of(transaction));
        when(courseService.getCourseEntityById(101L))
                .thenReturn(course);

        List<PaymentHistoryResponseDto> result = paymentService.getPaymentHistory(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getCourseId());
        assertEquals("Java Course", result.get(0).getCourseTitle());
        assertEquals(PaymentStatus.PENDING, result.get(0).getStatus());
    }

    @Test
    void verifyPayment_shouldReturnOrderDetails_whenAlreadySuccess() {
        transaction.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findByStripeSessionId("sess_123"))
                .thenReturn(Optional.of(transaction));

        PaymentOrderResponseDto result = paymentService.verifyPayment("sess_123");

        assertNotNull(result);
        assertEquals("sess_123", result.getStripeSessionId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(BigDecimal.valueOf(500), result.getAmount());
        assertEquals("INR", result.getCurrency());
    }

    @Test
    void verifyPayment_shouldReturnPending_whenStripeFails() {
        transaction.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByStripeSessionId("sess_123"))
                .thenReturn(Optional.of(transaction));

        PaymentOrderResponseDto result = paymentService.verifyPayment("sess_123");

        assertNotNull(result);
        assertEquals("sess_123", result.getStripeSessionId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(BigDecimal.valueOf(500), result.getAmount());
        assertEquals("INR", result.getCurrency());
    }

    @Test
    void verifyPayment_shouldThrowException_whenNotFound() {
        when(paymentRepository.findByStripeSessionId("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.verifyPayment("invalid"));
    }
}