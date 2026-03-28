package com.lms.notification.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setup() {
        mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // manually set fromEmail (because @Value won't work in unit test)
        emailService = new EmailServiceImpl(mailSender);
        setField(emailService, "fromEmail", "test@gmail.com");
    }

    // helper to inject private field
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendRegistrationOtpEmail_shouldSendEmail() {
        emailService.sendRegistrationOtpEmail("test@gmail.com", "Priya", "123456");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendWelcomeEmail_shouldSendEmail() {
        emailService.sendWelcomeEmail("test@gmail.com", "Priya");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPasswordResetOtpEmail_shouldSendEmail() {
        emailService.sendPasswordResetOtpEmail("test@gmail.com", "Priya", "654321");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendCourseCreatedEmail_shouldSendEmail() {
        emailService.sendCourseCreatedEmail("test@gmail.com", "Priya", "Spring Boot");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEnrollmentEmail_shouldSendEmail() {
        emailService.sendEnrollmentEmail("test@gmail.com", "Priya", "Java Course");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentSuccessEmail_shouldSendEmail() {
        emailService.sendPaymentSuccessEmail(
                "test@gmail.com",
                "Priya",
                "AWS Course",
                BigDecimal.valueOf(999),
                "INR",
                "PAY123"
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendEmail_shouldNotThrow_whenExceptionOccurs() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail error"));

        emailService.sendWelcomeEmail("test@gmail.com", "Priya");

        // no exception = pass
    }
}