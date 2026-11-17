package com.sapt.notification.gmail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ============================================================
 * GmailOAuthService - OAuth 2.0 Token Lifecycle Manager
 * ============================================================
 * Manages Google OAuth 2.0 access token retrieval, in-memory caching,
 * and automatic refresh before expiration for the Gmail API.
 *
 * Security & Reliability:
 *  - Thread-safe caching with ReentrantLock (prevents token stampede)
 *  - Caches access tokens in memory; refreshes 60 seconds before expiration
 *  - Never logs Client Secrets, Refresh Tokens, or Access Tokens
 *  - Supports forced token refresh on HTTP 401 responses
 * ============================================================
 */
@Slf4j
@Service
public class GmailOAuthService {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final long EXPIRY_SAFETY_BUFFER_SECONDS = 60; // Refresh 60s before actual expiry

    @Value("${gmail.client.id:${GMAIL_CLIENT_ID:}}")
    private String clientId;

    @Value("${gmail.client.secret:${GMAIL_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${gmail.refresh.token:${GMAIL_REFRESH_TOKEN:}}")
    private String refreshToken;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // In-memory token cache
    private volatile String cachedAccessToken;
    private volatile Instant tokenExpiryTime = Instant.MIN;

    @Autowired
    public GmailOAuthService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Package-private constructor for unit testing with custom HttpClient and properties.
     */
    GmailOAuthService(ObjectMapper objectMapper, HttpClient httpClient,
                      String clientId, String clientSecret, String refreshToken) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
    }

    /**
     * Checks if all required Google OAuth credentials are provided.
     */
    public boolean hasValidConfiguration() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()
                && refreshToken != null && !refreshToken.isBlank();
    }

    /**
     * Retrieves a valid Google OAuth access token, using the in-memory cache if
     * still unexpired, or requesting a fresh token from Google's OAuth endpoint.
     *
     * @return Valid access token
     * @throws IllegalStateException if configuration is missing or token exchange fails
     */
    public String getAccessToken() {
        if (!hasValidConfiguration()) {
            throw new IllegalStateException("Gmail OAuth configuration is incomplete. "
                    + "GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, and GMAIL_REFRESH_TOKEN must be configured.");
        }

        // Fast read: return cached token if still valid
        if (isTokenValid()) {
            return cachedAccessToken;
        }

        // Acquire lock to ensure only one thread requests a fresh token
        tokenLock.lock();
        try {
            // Double-check: another thread might have refreshed the token while we were waiting
            if (isTokenValid()) {
                return cachedAccessToken;
            }

            return requestNewAccessToken();
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Forces an immediate token refresh (e.g., after an HTTP 401 Unauthorized from Gmail API).
     * Invalidates the cached token and fetches a new one.
     */
    public String refreshAccessToken() {
        tokenLock.lock();
        try {
            log.info("Invalidating cached Gmail access token and requesting fresh token from Google OAuth...");
            this.cachedAccessToken = null;
            this.tokenExpiryTime = Instant.MIN;
            return requestNewAccessToken();
        } finally {
            tokenLock.unlock();
        }
    }

    private boolean isTokenValid() {
        return cachedAccessToken != null
                && Instant.now().plusSeconds(EXPIRY_SAFETY_BUFFER_SECONDS).isBefore(tokenExpiryTime);
    }

    private String requestNewAccessToken() {
        log.info("Requesting fresh Google OAuth access token for Gmail API...");

        try {
            String formBody = "client_id=" + URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret.trim(), StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(refreshToken.trim(), StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GOOGLE_TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String responseBody = response.body();

            if (statusCode == 200) {
                JsonNode json = objectMapper.readTree(responseBody);
                String newAccessToken = json.path("access_token").asText(null);
                long expiresInSeconds = json.path("expires_in").asLong(3600);

                if (newAccessToken == null || newAccessToken.isBlank()) {
                    throw new IllegalStateException("Google OAuth response did not contain 'access_token'");
                }

                this.cachedAccessToken = newAccessToken;
                this.tokenExpiryTime = Instant.now().plusSeconds(expiresInSeconds);

                log.info("✅ Successfully acquired Google OAuth access token (expires in {}s)", expiresInSeconds);
                return this.cachedAccessToken;
            } else {
                log.error("❌ Google OAuth token exchange failed: HTTP status={}, response={}", statusCode, responseBody);
                throw new IllegalStateException("Failed to exchange Gmail refresh token for access token (HTTP "
                        + statusCode + "): " + responseBody);
            }
        } catch (Exception e) {
            log.error("❌ Exception during Google OAuth token refresh: {}", e.getMessage());
            throw new IllegalStateException("Error contacting Google OAuth token endpoint: " + e.getMessage(), e);
        }
    }

    /**
     * Visible for testing: inspect cached expiry time.
     */
    Instant getTokenExpiryTime() {
        return tokenExpiryTime;
    }

    /**
     * Visible for testing: inspect cached token value.
     */
    String getCachedAccessToken() {
        return cachedAccessToken;
    }
}
