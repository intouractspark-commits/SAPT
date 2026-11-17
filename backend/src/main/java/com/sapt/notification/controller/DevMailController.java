package com.sapt.notification.controller;

import com.sapt.notification.gmail.GmailEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================
 * DevMailController - Development-Only Gmail API Email Test Endpoint
 * ============================================================
 * Allows manual verification of Gmail API OAuth 2.0 delivery.
 *
 * Usage:
 *   GET /api/dev/test-email?to=your-email@example.com
 *
 * Returns:
 *   200 OK: "Email sent successfully"
 *   400/500: "Email failed: <reason>"
 * ============================================================
 */
@Slf4j
@RestController
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevMailController {

    private final GmailEmailService gmailEmailService;

    @GetMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam(name = "to", required = false) String to) {
        if (to == null || to.trim().isBlank()) {
            return ResponseEntity.badRequest().body("Please specify a recipient email: /dev/test-email?to=your-email@example.com");
        }

        String recipient = to.trim();
        log.info("Triggered dev test email to: {}", recipient);

        String subject = "SAPT - Gmail API Test Email";
        String htmlBody = """
            <div style="font-family: Arial, sans-serif; max-width: 500px; margin: 20px auto; padding: 25px; border-radius: 12px; background: #1c1c1e; color: #f0f0f0; border: 1px solid #FF6B35;">
                <h2 style="color: #FF6B35; margin-top: 0;">SAPT &bull; Gmail API Test</h2>
                <p>Hello,</p>
                <p>This is a test email sent from <strong>SAPT Backend</strong> via the official <strong>Google Gmail REST API (OAuth 2.0)</strong>.</p>
                <div style="background: #2c2c2e; padding: 12px; border-radius: 8px; margin: 16px 0; border-left: 4px solid #30d158;">
                    <span style="color: #30d158; font-weight: bold;">&#10003; Gmail API Connected Successfully</span>
                </div>
                <p style="color: #888; font-size: 12px;">Timestamp: %s</p>
            </div>
            """.formatted(java.time.LocalDateTime.now().toString());

        boolean success = gmailEmailService.sendHtmlEmail(recipient, subject, htmlBody, "DEV_TEST");

        if (success) {
            return ResponseEntity.ok("Email sent successfully to: " + recipient);
        } else {
            return ResponseEntity.internalServerError().body("Email failed: check server logs for Gmail API response details.");
        }
    }
}
