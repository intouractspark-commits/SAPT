package com.sapt.notification.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GmailEmailServiceTest {

    @Mock
    private GmailOAuthService mockOAuthService;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private ObjectMapper objectMapper;
    private GmailEmailService emailService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailService = new GmailEmailService(
                mockOAuthService,
                objectMapper,
                mockHttpClient,
                "intouract.spark@gmail.com"
        );
    }

    @Test
    void testCreateMimeMessage_FormatAndHeaders() {
        String from = "intouract.spark@gmail.com";
        String to = "student@example.com";
        String subject = "SAPT - Verify Your Email";
        String htmlBody = "<h1>Your OTP: 123456</h1>";

        String mime = emailService.createMimeMessage(from, to, subject, htmlBody);

        assertTrue(mime.contains("From: SAPT <intouract.spark@gmail.com>"));
        assertTrue(mime.contains("To: student@example.com"));
        assertTrue(mime.contains("Subject: =?UTF-8?B?"));
        assertTrue(mime.contains("MIME-Version: 1.0"));
        assertTrue(mime.contains("Content-Type: text/html; charset=UTF-8"));
        assertTrue(mime.contains("Content-Transfer-Encoding: 8bit"));
        assertTrue(mime.contains(htmlBody));
    }

    @Test
    void testEncodeMimeToUrlSafeBase64_NoPaddingAndUrlSafe() {
        String testInput = "Hello World! Special characters: + / = ? & % #";
        String encoded = emailService.encodeMimeToUrlSafeBase64(testInput);

        // Must not contain padding '='
        assertFalse(encoded.contains("="));
        // Must not contain standard base64 '+' or '/'
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));

        // Decoding URL-safe must restore original bytes
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encoded);
        assertEquals(testInput, new String(decodedBytes, StandardCharsets.UTF_8));
    }

    @Test
    void testSendHtmlEmail_Success() throws Exception {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(true);
        when(mockOAuthService.getAccessToken()).thenReturn("mock-access-token-123");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"id\":\"msg_98765\",\"threadId\":\"th_98765\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        boolean result = emailService.sendHtmlEmail(
                "student@example.com",
                "Your OTP",
                "<p>123456</p>",
                "OTP"
        );

        assertTrue(result);
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    void testSendHtmlEmail_Http401_TriggersTokenRefreshAndRetrySuccess() throws Exception {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(true);
        when(mockOAuthService.getAccessToken()).thenReturn("expired-token");
        when(mockOAuthService.refreshAccessToken()).thenReturn("fresh-token-456");

        HttpResponse<String> response401 = mock(HttpResponse.class);
        when(response401.statusCode()).thenReturn(401);
        when(response401.body()).thenReturn("{\"error\":{\"code\":401,\"message\":\"Invalid Credentials\"}}");

        HttpResponse<String> response200 = mock(HttpResponse.class);
        when(response200.statusCode()).thenReturn(200);
        when(response200.body()).thenReturn("{\"id\":\"msg_retry_success\"}");

        // First call returns 401, second call returns 200
        doReturn(response401, response200).when(mockHttpClient).send(any(), any());

        boolean result = emailService.sendHtmlEmail(
                "student@example.com",
                "Your OTP",
                "<p>123456</p>",
                "OTP"
        );

        assertTrue(result);
        verify(mockOAuthService, times(1)).refreshAccessToken();
        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void testSendHtmlEmail_Http400_ReturnsFalseWithoutThrowing() throws Exception {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(true);
        when(mockOAuthService.getAccessToken()).thenReturn("valid-token");

        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"code\":400,\"message\":\"Invalid recipient\"}}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        boolean result = emailService.sendHtmlEmail(
                "bad-email",
                "Your OTP",
                "<p>123456</p>",
                "OTP"
        );

        assertFalse(result);
    }

    @Test
    void testSendHtmlEmail_Http403_Forbidden_ReturnsFalse() throws Exception {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(true);
        when(mockOAuthService.getAccessToken()).thenReturn("valid-token");

        when(mockHttpResponse.statusCode()).thenReturn(403);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"code\":403,\"message\":\"Quota exceeded\"}}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        boolean result = emailService.sendHtmlEmail(
                "student@example.com",
                "Subject",
                "<p>Body</p>",
                "OTP"
        );

        assertFalse(result);
    }

    @Test
    void testSendHtmlEmail_Http429_RateLimited_ReturnsFalse() throws Exception {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(true);
        when(mockOAuthService.getAccessToken()).thenReturn("valid-token");

        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpResponse.body()).thenReturn("{\"error\":{\"code\":429,\"message\":\"Rate limit exceeded\"}}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        boolean result = emailService.sendHtmlEmail(
                "student@example.com",
                "Subject",
                "<p>Body</p>",
                "OTP"
        );

        assertFalse(result);
    }

    @Test
    void testSendHtmlEmail_EmptyRecipient_FailsFast() {
        boolean result = emailService.sendHtmlEmail("", "Subject", "<p>Body</p>", "OTP");
        assertFalse(result);
    }

    @Test
    void testSendHtmlEmail_MissingConfiguration_FailsGracefully() {
        when(mockOAuthService.hasValidConfiguration()).thenReturn(false);

        boolean result = emailService.sendHtmlEmail("student@example.com", "Subject", "<p>Body</p>", "OTP");
        assertFalse(result);
    }
}
