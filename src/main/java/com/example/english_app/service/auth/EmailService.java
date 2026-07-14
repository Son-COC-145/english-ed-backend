package com.example.english_app.service.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Gửi email reset password
    @Async
    public void sendResetPasswordEmail(String email,
            String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Đặt lại mật khẩu");
            message.setText(
                    "Nhấn vào link để đặt lại mật khẩu:\n\n"
                            + "http://localhost:3000/reset-password?token="
                            + resetToken
                            + "\n\nLink có hiệu lực trong 15 phút.");
            mailSender.send(message);
            log.info("Reset password email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}", email, e);
        }
    }

    // Gửi email chào mừng
    @Async
    public void sendWelcomeEmail(String email, String fullName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Chào mừng đến với English App");
            message.setText(
                    "Xin chào " + fullName + ",\n\n"
                            + "Tài khoản của bạn đã được tạo thành công.");
            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", email);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}", email, e);
        }
    }
}
