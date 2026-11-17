package com.sapt.notification.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapt.notification.templates.MailTemplates;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GmailEmailService - Gmail REST API Email Dispatcher (OAuth 2.0)
 * ============================================================
 * Delivers all transactional emails via the official Google Gmail API:
 *   POST https://gmail.googleapis.com/gmail/v1/users/me/messages/send
 *
 * Architecture & Features:
 *  - Automatic OAuth access token acquisition & renewal (via GmailOAuthService)
 *  - RFC 2822 / MIME compliant email construction with UTF-8 encoding
 *  - URL-safe Base64 encoding without padding (RFC 4648 §5)
 *  - Automatic HTTP 401 token refresh & single retry
 *  - Complete preservation of public API methods for auth/OTP/welcome flows
 *  - Fails gracefully without throwing unhandled exceptions to callers
 * ============================================================
 */
@Slf4j
@Service
public class GmailEmailService {

    private static final String GMAIL_SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    @Value("${gmail.from:${GMAIL_FROM:intouract.spark@gmail.com}}")
    private String fromAddress;

    private final GmailOAuthService gmailOAuthService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GmailEmailService(GmailOAuthService gmailOAuthService, ObjectMapper objectMapper) {
        this.gmailOAuthService = gmailOAuthService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Package-private constructor for testing with custom HttpClient and properties.
     */
    GmailEmailService(GmailOAuthService gmailOAuthService, ObjectMapper objectMapper,
                      HttpClient httpClient, String fromAddress) {
        this.gmailOAuthService = gmailOAuthService;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.fromAddress = fromAddress;
    }

    /**
     * Sends an HTML email via the Gmail REST API synchronously with boolean result.
     * Automatically handles OAuth token resolution and 401 retry.
     *
     * @param to        Recipient email address
     * @param subject   Email subject line
     * @param htmlBody  HTML body content
     * @param emailType Logical classification for logging (e.g. OTP, WELCOME)
     * @return true if Gmail accepted the message, false otherwise
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlBody, String emailType) {
        String type = (emailType != null && !emailType.isBlank()) ? emailType : "TRANSACTIONAL";

        if (to == null || to.isBlank()) {
            log.warn("❌ Gmail API email failed [type={}]: Recipient address is empty.", type);
            return false;
        }

        if (!gmailOAuthService.hasValidConfiguration()) {
            log.error("❌ Gmail API email failed [type={}] for {}: Gmail OAuth credentials are not fully configured.",
                    type, to);
            return false;
        }

        try {
            // Build RFC 2822 MIME message and URL-safe Base64 encode
            String rawBase64 = encodeMimeToUrlSafeBase64(createMimeMessage(fromAddress, to.trim(), subject, htmlBody));

            // Attempt delivery with current access token
            String accessToken = gmailOAuthService.getAccessToken();
            HttpResponse<String> response = executeSendRequest(rawBase64, accessToken);

            int statusCode = response.statusCode();

            // Handle HTTP 401: token expired/invalidated -> refresh and retry ONCE
            if (statusCode == 401) {
                log.warn("⚠️ Received HTTP 401 from Gmail API for [type={}] {}. Invaliding token cache and retrying once...",
                        type, to);
                accessToken = gmailOAuthService.refreshAccessToken();
                response = executeSendRequest(rawBase64, accessToken);
                statusCode = response.statusCode();
            }

            return evaluateResponse(statusCode, response.body(), to, type);

        } catch (Exception e) {
            log.error("❌ Gmail API email delivery exception [type={}] for {}: {}", type, to, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Sends an HTML email asynchronously (fire-and-forget).
     */
    @Async
    public void sendHtmlEmailAsync(String to, String subject, String htmlBody, String emailType) {
        sendHtmlEmail(to, subject, htmlBody, emailType);
    }

    /**
     * Sends an OTP email for verification or password reset.
     */
    @Async
    public void sendOtpEmail(String to, String fullName, String otp, String purpose) {
        try {
            String subject = "SAPT - " + purpose + " OTP";
            String htmlBody = MailTemplates.buildOtpEmail(otp, purpose, fullName);
            sendHtmlEmail(to, subject, htmlBody, "OTP_" + purpose.toUpperCase().replace(' ', '_'));
        } catch (Exception e) {
            log.error("❌ Gmail API email failed [type=OTP] for {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends email address verification OTP.
     */
    @Async
    public void sendVerificationEmail(String to, String fullName, String otp) {
        sendOtpEmail(to, fullName, otp, "Email Verification");
    }

    /**
     * Sends password reset OTP.
     */
    @Async
    public void sendPasswordResetEmail(String to, String otp) {
        sendOtpEmail(to, null, otp, "Password Reset");
    }

    /**
     * Sends welcome email upon account approval or setup.
     */
    @Async
    public void sendWelcomeEmail(String to, String fullName, String role) {
        try {
            String subject = "Welcome to SAPT - Student Activity & Performance Tracker";
            String htmlBody = MailTemplates.buildWelcomeEmail(fullName, role);
            sendHtmlEmail(to, subject, htmlBody, "WELCOME");
        } catch (Exception e) {
            log.error("❌ Gmail API email failed [type=WELCOME] for {}: {}", to, e.getMessage());
        }
    }

    /**
     * Sends college admin invitation/verification email.
     */
    @Async
    public void sendCollegeAdminVerificationEmail(String to, String fullName, String password, String verifyLink) {
        try {
            String subject = "SAPT - College Admin Registration & Verification";
            String htmlBody = MailTemplates.buildCollegeAdminWelcomeEmail(fullName, to, password, verifyLink);
            sendHtmlEmail(to, subject, htmlBody, "COLLEGE_ADMIN_VERIFICATION");
        } catch (Exception e) {
            log.error("❌ Gmail API email failed [type=COLLEGE_ADMIN_VERIFICATION] for {}: {}", to, e.getMessage());
        }
    }

    // ============================================================
    // HTTP EXECUTION & RESPONSE EVALUATION
    // ============================================================

    private HttpResponse<String> executeSendRequest(String rawBase64Message, String accessToken) throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("raw", rawBase64Message);
        String requestJson = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GMAIL_SEND_URL))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("User-Agent", "SAPT-Backend-Gmail/1.0")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private boolean evaluateResponse(int statusCode, String responseBody, String recipient, String type) {
        if (statusCode >= 200 && statusCode < 300) {
            String messageId = extractMessageId(responseBody);
            log.info("✅ Gmail API email sent [type={}] to {}: status={}, messageId={}",
                    type, recipient, statusCode, messageId);
            return true;
        }

        switch (statusCode) {
            case 400 -> log.error("❌ Gmail API email failed [type={}] for {}: Bad Request (HTTP 400). Response: {}",
                    type, recipient, responseBody);
            case 401 -> log.error("❌ Gmail API email failed [type={}] for {}: Unauthorized (HTTP 401). Response: {}",
                    type, recipient, responseBody);
            case 403 -> log.error("❌ Gmail API email failed [type={}] for {}: Forbidden/Quota exceeded (HTTP 403). Response: {}",
                    type, recipient, responseBody);
            case 429 -> log.error("❌ Gmail API email failed [type={}] for {}: Rate limited (HTTP 429). Response: {}",
                    type, recipient, responseBody);
            default  -> log.error("❌ Gmail API email failed [type={}] for {}: HTTP status={}, response={}",
                    type, recipient, statusCode, responseBody);
        }
        return false;
    }

    private String extractMessageId(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return node.path("id").asText("unknown");
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ============================================================
    // MIME / RFC 2822 BUILDER & ENCODER
    // ============================================================

    /**
     * Constructs a standard RFC 2822 formatted email message string.
     */
    String createMimeMessage(String from, String to, String subject, String htmlBody) {
        String effectiveFrom = (from != null && !from.isBlank()) ? from.trim() : "intouract.spark@gmail.com";
        String senderHeader = effectiveFrom.contains("<")
                ? effectiveFrom
                : "SAPT <" + effectiveFrom + ">";

        // Encode subject using RFC 2047 Base64
        String encodedSubject = "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString((subject != null ? subject : "").getBytes(StandardCharsets.UTF_8))
                + "?=";

        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(senderHeader).append("\r\n");
        sb.append("To: ").append(to).append("\r\n");
        sb.append("Subject: ").append(encodedSubject).append("\r\n");
        sb.append("MIME-Version: 1.0\r\n");
        sb.append("Content-Type: text/html; charset=UTF-8\r\n");
        sb.append("Content-Transfer-Encoding: 8bit\r\n");
        sb.append("\r\n");
        sb.append(htmlBody != null ? htmlBody : "");

        return sb.toString();
    }

    /**
     * Encodes RFC 2822 MIME message into URL-safe Base64 without padding (RFC 4648 §5).
     */
    String encodeMimeToUrlSafeBase64(String mimeMessage) {
        byte[] bytes = mimeMessage.getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
