package com.sapt.notification.otp;

import com.sapt.notification.gmail.GmailEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * OtpMailService - OTP Email Notification Service (Gmail API)
 * ============================================================
 * Responsible for sending OTP emails for:
 *  - Email verification (during registration)
 *  - Password reset (forgot password flow)
 *
 * Dispatches via Google Gmail REST API (GmailEmailService).
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpMailService {

    private final GmailEmailService gmailEmailService;

    /**
     * Sends an OTP email for email address verification.
     * Called after user registration.
     *
     * @param toEmail  Recipient's email address
     * @param fullName Recipient's display name (used in email body)
     * @param otp      The 6-digit OTP string
     */
    @Async
    public void sendEmailVerificationOtp(String toEmail, String fullName, String otp) {
        try {
            gmailEmailService.sendVerificationEmail(toEmail, fullName, otp);
        } catch (Exception e) {
            log.error("Failed to send email verification OTP to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends an OTP email for password reset.
     * Called when user requests forgot-password flow.
     *
     * @param toEmail Recipient's email address
     * @param otp     The 6-digit OTP string
     */
    @Async
    public void sendPasswordResetOtp(String toEmail, String otp) {
        try {
            gmailEmailService.sendPasswordResetEmail(toEmail, otp);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to {}: {}", toEmail, e.getMessage());
        }
    }
}
