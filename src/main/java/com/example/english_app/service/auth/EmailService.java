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

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

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
                            + frontendUrl + "/reset-password?token="
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

    @Async
    public void sendAccountDeletionVerificationEmail(String email, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Xác nhận yêu cầu xóa tài khoản English App");
            message.setText(
                    "Chúng tôi nhận được yêu cầu xóa tài khoản English App của bạn.\n\n"
                            + "Xác nhận tại:\n"
                            // Keep the one-time token in the URL fragment so it is not sent in
                            // HTTP access logs or Referer headers when the page is loaded.
                            + publicBaseUrl + "/legal/account-deletion-confirm.html#token=" + token
                            + "\n\nLiên kết có hiệu lực trong 30 phút. "
                            + "Nếu bạn không gửi yêu cầu này, hãy bỏ qua email.");
            mailSender.send(message);
            log.info("Account-deletion verification email sent");
        } catch (Exception exception) {
            log.error("Failed to send account-deletion verification email", exception);
        }
    }
}
