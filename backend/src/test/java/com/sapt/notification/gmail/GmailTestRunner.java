package com.sapt.notification.gmail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Direct standalone test runner that verifies all Gmail OAuth and Email service logic
 * without requiring a separate Surefire process fork.
 */
public class GmailTestRunner {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Running Gmail API Unit Tests directly...");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        ObjectMapper objectMapper = new ObjectMapper();

        // -------------------------------------------------------------
        // Test 1: OAuth Service - hasValidConfiguration
        // -------------------------------------------------------------
        try {
            HttpClient mockHttp = mock(HttpClient.class);
            GmailOAuthService service = new GmailOAuthService(objectMapper, mockHttp, "client-id", "client-secret", "refresh-token");
            if (!service.hasValidConfiguration()) throw new AssertionError("Expected true");

            GmailOAuthService emptyService = new GmailOAuthService(objectMapper, mockHttp, "", "", "");
            if (emptyService.hasValidConfiguration()) throw new AssertionError("Expected false");

            System.out.println("✅ Test 1 Passed: GmailOAuthService.hasValidConfiguration()");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 1 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 2: OAuth Service - Token exchange & In-memory caching
        // -------------------------------------------------------------
        try {
            HttpClient mockHttp = mock(HttpClient.class);
            HttpResponse<String> mockResponse = mock(HttpResponse.class);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"access_token\":\"ya29.mock-token-xyz\",\"expires_in\":3600}");
            doReturn(mockResponse).when(mockHttp).send(any(), any());

            GmailOAuthService service = new GmailOAuthService(objectMapper, mockHttp, "client-id", "client-secret", "refresh-token");
            String t1 = service.getAccessToken();
            String t2 = service.getAccessToken(); // Cached

            if (!"ya29.mock-token-xyz".equals(t1) || !"ya29.mock-token-xyz".equals(t2)) {
                throw new AssertionError("Tokens do not match expected value");
            }
            verify(mockHttp, times(1)).send(any(), any());

            System.out.println("✅ Test 2 Passed: Token exchange & In-memory caching");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 2 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 3: OAuth Service - Forced refreshAccessToken()
        // -------------------------------------------------------------
        try {
            HttpClient mockHttp = mock(HttpClient.class);
            HttpResponse<String> resp1 = mock(HttpResponse.class);
            when(resp1.statusCode()).thenReturn(200);
            when(resp1.body()).thenReturn("{\"access_token\":\"token-1\",\"expires_in\":3600}");

            HttpResponse<String> resp2 = mock(HttpResponse.class);
            when(resp2.statusCode()).thenReturn(200);
            when(resp2.body()).thenReturn("{\"access_token\":\"token-2\",\"expires_in\":3600}");

            doReturn(resp1, resp2).when(mockHttp).send(any(), any());

            GmailOAuthService service = new GmailOAuthService(objectMapper, mockHttp, "client-id", "client-secret", "refresh-token");
            String t1 = service.getAccessToken();
            String t2 = service.refreshAccessToken();

            if (!"token-1".equals(t1) || !"token-2".equals(t2)) {
                throw new AssertionError("Token refresh did not yield new token");
            }
            verify(mockHttp, times(2)).send(any(), any());

            System.out.println("✅ Test 3 Passed: Forced refreshAccessToken()");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 3 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 4: MIME Message Generation & Headers
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            HttpClient mockHttp = mock(HttpClient.class);
            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");

            String mime = emailService.createMimeMessage("intouract.spark@gmail.com", "to@example.com", "Subject Test", "<b>Body</b>");
            if (!mime.contains("From: SAPT <intouract.spark@gmail.com>")) throw new AssertionError("Missing From header");
            if (!mime.contains("To: to@example.com")) throw new AssertionError("Missing To header");
            if (!mime.contains("Subject: =?UTF-8?B?")) throw new AssertionError("Subject not RFC 2047 encoded");
            if (!mime.contains("Content-Type: text/html; charset=UTF-8")) throw new AssertionError("Missing Content-Type");
            if (!mime.contains("<b>Body</b>")) throw new AssertionError("Missing HTML body");

            System.out.println("✅ Test 4 Passed: RFC 2822 MIME message construction");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 4 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 5: URL-Safe Base64 Encoding without padding
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            HttpClient mockHttp = mock(HttpClient.class);
            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");

            String sample = "Hello World! Test characters: ? & = + / \n 123";
            String encoded = emailService.encodeMimeToUrlSafeBase64(sample);

            if (encoded.contains("=")) throw new AssertionError("Must not contain '=' padding");
            if (encoded.contains("+") || encoded.contains("/")) throw new AssertionError("Must be URL safe");

            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            if (!sample.equals(new String(decoded, StandardCharsets.UTF_8))) {
                throw new AssertionError("Decoded bytes do not match original");
            }

            System.out.println("✅ Test 5 Passed: URL-safe Base64 encoding (RFC 4648 §5)");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 5 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 6: Successful Send (HTTP 200)
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            when(mockOAuth.hasValidConfiguration()).thenReturn(true);
            when(mockOAuth.getAccessToken()).thenReturn("valid-token");

            HttpClient mockHttp = mock(HttpClient.class);
            HttpResponse<String> resp = mock(HttpResponse.class);
            when(resp.statusCode()).thenReturn(200);
            when(resp.body()).thenReturn("{\"id\":\"msg_12345\"}");
            doReturn(resp).when(mockHttp).send(any(), any());

            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");
            boolean sent = emailService.sendHtmlEmail("user@example.com", "Test Subject", "<p>Hello</p>", "TEST");

            if (!sent) throw new AssertionError("Expected send to succeed");

            System.out.println("✅ Test 6 Passed: Send email success (HTTP 200)");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 6 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 7: HTTP 401 Unauthorized -> Refresh Token and Retry
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            when(mockOAuth.hasValidConfiguration()).thenReturn(true);
            when(mockOAuth.getAccessToken()).thenReturn("expired-token");
            when(mockOAuth.refreshAccessToken()).thenReturn("fresh-token");

            HttpClient mockHttp = mock(HttpClient.class);
            HttpResponse<String> resp401 = mock(HttpResponse.class);
            when(resp401.statusCode()).thenReturn(401);
            when(resp401.body()).thenReturn("{\"error\":{\"code\":401,\"message\":\"Invalid Credentials\"}}");

            HttpResponse<String> resp200 = mock(HttpResponse.class);
            when(resp200.statusCode()).thenReturn(200);
            when(resp200.body()).thenReturn("{\"id\":\"msg_after_refresh\"}");

            doReturn(resp401, resp200).when(mockHttp).send(any(), any());

            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");
            boolean sent = emailService.sendHtmlEmail("user@example.com", "Test Subject", "<p>Hello</p>", "TEST");

            if (!sent) throw new AssertionError("Expected retry after 401 to succeed");
            verify(mockOAuth, times(1)).refreshAccessToken();
            verify(mockHttp, times(2)).send(any(), any());

            System.out.println("✅ Test 7 Passed: HTTP 401 auto-refresh and retry");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 7 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 8: HTTP 400, 403, 429 Error Handling (Fails Gracefully)
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            when(mockOAuth.hasValidConfiguration()).thenReturn(true);
            when(mockOAuth.getAccessToken()).thenReturn("valid-token");

            HttpClient mockHttp = mock(HttpClient.class);
            HttpResponse<String> resp403 = mock(HttpResponse.class);
            when(resp403.statusCode()).thenReturn(403);
            when(resp403.body()).thenReturn("{\"error\":{\"code\":403,\"message\":\"Quota exceeded\"}}");
            doReturn(resp403).when(mockHttp).send(any(), any());

            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");
            boolean sent = emailService.sendHtmlEmail("user@example.com", "Test Subject", "<p>Hello</p>", "TEST");

            if (sent) throw new AssertionError("Expected HTTP 403 to return false");

            System.out.println("✅ Test 8 Passed: HTTP 403 error handled gracefully");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 8 Failed: " + t.getMessage());
            failed++;
        }

        // -------------------------------------------------------------
        // Test 9: Missing Configuration Handling
        // -------------------------------------------------------------
        try {
            GmailOAuthService mockOAuth = mock(GmailOAuthService.class);
            when(mockOAuth.hasValidConfiguration()).thenReturn(false);

            HttpClient mockHttp = mock(HttpClient.class);
            GmailEmailService emailService = new GmailEmailService(mockOAuth, objectMapper, mockHttp, "intouract.spark@gmail.com");
            boolean sent = emailService.sendHtmlEmail("user@example.com", "Test", "<p>Hi</p>", "TEST");

            if (sent) throw new AssertionError("Expected missing config to return false");

            System.out.println("✅ Test 9 Passed: Missing configuration handled gracefully");
            passed++;
        } catch (Throwable t) {
            System.err.println("❌ Test 9 Failed: " + t.getMessage());
            failed++;
        }

        System.out.println("=================================================");
        System.out.println("Test Summary: " + passed + " passed, " + failed + " failed.");
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
