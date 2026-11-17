package com.sapt.notification.mail;

import com.sapt.notification.gmail.GmailEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * MailService - Email Dispatch Service (Gmail REST API Backend)
 * ============================================================
 * Legacy SMTP and Resend have been completely replaced.
 * All outgoing emails are routed via official Google Gmail API
 * through GmailEmailService.
 *
 * This class preserves method signatures (sendHtmlMail, sendPlainMail)
 * so existing callers (NotificationService, SystemAdminServiceImpl, etc.)
 * require zero breaking changes.
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final GmailEmailService gmailEmailService;

    /**
     * Sends an HTML email asynchronously via Gmail REST API.
     *
     * @param to       Recipient email address
     * @param subject  Email subject line
     * @param htmlBody HTML content of the email body
     */
    @Async
    public void sendHtmlMail(String to, String subject, String htmlBody) {
        try {
            gmailEmailService.sendHtmlEmail(to, subject, htmlBody, "TRANSACTIONAL");
        } catch (Exception e) {
            log.error("❌ Gmail API email failed in MailService for {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends a plain text email asynchronously via Gmail REST API.
     * Converts plain text into HTML paragraph for delivery.
     *
     * @param to      Recipient email address
     * @param subject Email subject line
     * @param text    Plain text content
     */
    @Async
    public void sendPlainMail(String to, String subject, String text) {
        try {
            String htmlBody = "<div style=\"font-family:sans-serif;font-size:14px;color:#333;line-height:1.5;\">"
                    + (text != null ? text.replace("\n", "<br/>") : "")
                    + "</div>";
            gmailEmailService.sendHtmlEmail(to, subject, htmlBody, "PLAIN_TEXT");
        } catch (Exception e) {
            log.error("❌ Gmail API email failed in MailService (plain) for {}: {}", to, e.getMessage());
        }
    }
}
