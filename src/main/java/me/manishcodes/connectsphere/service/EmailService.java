package me.manishcodes.connectsphere.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String toEmail, String username, String token) {
        String verifyLink = baseUrl + "/api/v1/auth/verify-email?token=" + token;

        String htmlBody = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #23a0faff;">Welcome to ConnectSphere, %s! </h2>
                    <p>Thank you for registering. Please verify your email address by clicking the button below.</p>
                    <p>This link expires in <strong>24 hours</strong>.</p>
                    <a href="%s"
                       style="display:inline-block; padding:12px 24px; background:#4F46E5;
                              color:#fff; text-decoration:none; border-radius:6px; font-weight:bold;">
                        Verify My Email
                    </a>
                    <p style="margin-top:20px; color:#888; font-size:12px;">
                        If you did not register, you can safely ignore this email.
                    </p>
                </div>
                """.formatted(username, verifyLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your ConnectSphere account");
            helper.setText(htmlBody, true); // true = send as HTML

            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String token) {
        String resetLink = baseUrl + "/api/v1/auth/reset-password?token=" + token;

        String htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                <h2 style="color: #4F46E5;">Password Reset Request</h2>
                <p>Hi %s, we received a request to reset your ConnectSphere password.</p>
                <p>Click the button below to reset your password. This link expires in <strong>15 minutes</strong>.</p>
                <a href="%s"
                   style="display:inline-block; padding:12px 24px; background:#4F46E5;
                          color:#fff; text-decoration:none; border-radius:6px; font-weight:bold;">
                    Reset My Password
                </a>
                <p style="margin-top:20px; color:#888; font-size:12px;">
                    If you did not request a password reset, you can safely ignore this email.
                    Your password will not be changed.
                </p>
            </div>
            """.formatted(username, resetLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your ConnectSphere password");
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetConfirmationEmail(String toEmail, String username) {
        String htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                <h2 style="color: #4F46E5;">Password Changed Successfully</h2>
                <p>Hi %s, your ConnectSphere password has been reset successfully.</p>
                <p>If you did <strong>not</strong> make this change, please contact our support
                   immediately or reset your password again.</p>
                <p style="margin-top:20px; color:#888; font-size:12px;">
                    For security, this is an automated notification.
                </p>
            </div>
            """.formatted(username);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your ConnectSphere password was changed");
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Password reset confirmation email sent to {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }


}
