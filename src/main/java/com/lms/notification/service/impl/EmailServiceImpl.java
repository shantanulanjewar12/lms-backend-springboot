package com.lms.notification.service.impl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.lms.notification.service.EmailService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendRegistrationOtpEmail(String to, String fullName, String otp) {
        String subject = "Verify your email - Let's Learn";
        String body = buildTemplate(
                "Welcome to Let's Learn",
                "Hi " + safeName(fullName)
                        + ", your account is almost ready. Use the OTP below to verify your email.",
                "OTP: <strong style='font-size:22px;letter-spacing:3px;'>" + otp + "</strong>",
                "This OTP is valid for 10 minutes.");
        sendHtmlEmail(to, subject, body);
    }

    @Override
    public void sendWelcomeEmail(String to, String fullName) {
        String subject = "Welcome to Let's Learn";
        String body = buildTemplate(
                "Your account is ready",
                "Hi " + safeName(fullName) + ", your email has been verified successfully.",
                "You can now sign in and start learning from your dashboard.",
                "Happy learning with Let's Learn.");
        sendHtmlEmail(to, subject, body);
    }

    @Override
    public void sendPasswordResetOtpEmail(String to, String fullName, String otp) {
        String subject = "Password reset OTP - Let's Learn";
        String body = buildTemplate(
                "Reset your password",
                "Hi " + safeName(fullName)
                        + ", use the OTP below to verify your password reset request.",
                "OTP: <strong style='font-size:22px;letter-spacing:3px;'>" + otp + "</strong>",
                "This OTP is valid for 10 minutes. If you did not request this, please ignore this email.");
        sendHtmlEmail(to, subject, body);
    }

    @Override
    public void sendCourseCreatedEmail(String to, String fullName, String courseTitle) {
        String subject = "Course created successfully - Let's Learn";
        String body = buildTemplate(
                "Course Created",
                "Hi " + safeName(fullName) + ", your course has been created successfully.",
                "<strong>Course:</strong> " + escape(courseTitle),
                "You can now add lessons and publish it for learners.");
        sendHtmlEmail(to, subject, body);
    }

    @Override
    public void sendEnrollmentEmail(String to, String fullName, String courseTitle) {
        String subject = "Enrollment confirmed - Let's Learn";
        String body = buildTemplate(
                "You are enrolled",
                "Hi " + safeName(fullName) + ", enrollment was successful.",
                "<strong>Course:</strong> " + escape(courseTitle),
                "Start learning from your dashboard right away.");
        sendHtmlEmail(to, subject, body);
    }

    @Override
    public void sendPaymentSuccessEmail(String to, String fullName, String courseTitle, BigDecimal amount, String currency,
            String paymentId) {
        String subject = "Payment successful - Let's Learn";
        String body = buildTemplate(
                "Payment received",
                "Hi " + safeName(fullName) + ", your payment has been processed successfully.",
                "<strong>Course:</strong> " + escape(courseTitle)
                        + "<br/><strong>Amount:</strong> " + escape(currency) + " " + amount
                        + "<br/><strong>Payment ID:</strong> " + escape(paymentId),
                "Thank you for learning with Let's Learn.");
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send email to {} with subject {}: {}", to, subject, ex.getMessage());
        }
    }

    private String buildTemplate(String heading, String intro, String content, String footer) {
        return """
                <html>
                <body style='margin:0;padding:0;background:#f6f8fb;font-family:Arial,sans-serif;'>
                  <div style='max-width:620px;margin:24px auto;background:#ffffff;border:1px solid #e5eaf2;border-radius:14px;overflow:hidden;'>
                    <div style='padding:18px 22px;background:linear-gradient(135deg,#2563eb,#7c3aed);color:#fff;'>
                      <h2 style='margin:0;font-size:20px;'>Let's Learn</h2>
                    </div>
                    <div style='padding:24px 22px;color:#1f2937;'>
                      <h3 style='margin:0 0 10px 0;font-size:20px;'>%s</h3>
                      <p style='margin:0 0 14px 0;line-height:1.6;'>%s</p>
                      <div style='padding:14px;border:1px solid #dbe4f3;background:#f8fbff;border-radius:10px;line-height:1.6;'>%s</div>
                      <p style='margin:14px 0 0 0;color:#6b7280;font-size:13px;'>%s</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(heading, intro, content, footer);
    }

    private String safeName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Learner";
        }
        return escape(fullName);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
