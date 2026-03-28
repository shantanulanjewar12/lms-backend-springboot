package com.lms.notification.service;

import java.math.BigDecimal;

public interface EmailService {

    void sendRegistrationOtpEmail(String to, String fullName, String otp);

    void sendWelcomeEmail(String to, String fullName);

    void sendPasswordResetOtpEmail(String to, String fullName, String otp);

    void sendCourseCreatedEmail(String to, String fullName, String courseTitle);

    void sendEnrollmentEmail(String to, String fullName, String courseTitle);

    void sendPaymentSuccessEmail(String to, String fullName, String courseTitle, BigDecimal amount, String currency,
            String paymentId);
}
