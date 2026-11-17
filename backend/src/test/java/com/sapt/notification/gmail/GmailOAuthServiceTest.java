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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GmailOAuthServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private ObjectMapper objectMapper;
    private GmailOAuthService oAuthService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        oAuthService = new GmailOAuthService(
                objectMapper,
                mockHttpClient,
                "test-client-id.apps.googleusercontent.com",
                "test-client-secret",
                "test-refresh-token"
        );
    }

    @Test
    void testHasValidConfiguration_WhenConfigured_ReturnsTrue() {
        assertTrue(oAuthService.hasValidConfiguration());
    }

    @Test
    void testHasValidConfiguration_WhenMissing_ReturnsFalse() {
        GmailOAuthService emptyService = new GmailOAuthService(objectMapper, mockHttpClient, "", "", "");
        assertFalse(emptyService.hasValidConfiguration());
    }

    @Test
    void testGetAccessToken_Success_AndCaching() throws Exception {
        String jsonResponse = "{\"access_token\":\"ya29.mock-token-abc\",\"expires_in\":3600,\"token_type\":\"Bearer\"}";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(jsonResponse);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        // First call: fetches from Google
        String token1 = oAuthService.getAccessToken();
        assertEquals("ya29.mock-token-abc", token1);

        // Second call: should use cached token without making another HTTP call
        String token2 = oAuthService.getAccessToken();
        assertEquals("ya29.mock-token-abc", token2);

        // Verify only 1 HTTP request was made
        verify(mockHttpClient, times(1)).send(any(), any());
    }

    @Test
    void testRefreshAccessToken_ForcesNewTokenFetch() throws Exception {
        String response1 = "{\"access_token\":\"token-initial\",\"expires_in\":3600}";
        String response2 = "{\"access_token\":\"token-refreshed\",\"expires_in\":3600}";

        HttpResponse<String> mockResponse1 = mock(HttpResponse.class);
        when(mockResponse1.statusCode()).thenReturn(200);
        when(mockResponse1.body()).thenReturn(response1);

        HttpResponse<String> mockResponse2 = mock(HttpResponse.class);
        when(mockResponse2.statusCode()).thenReturn(200);
        when(mockResponse2.body()).thenReturn(response2);

        doReturn(mockResponse1, mockResponse2).when(mockHttpClient).send(any(), any());

        String initial = oAuthService.getAccessToken();
        assertEquals("token-initial", initial);

        // Force refresh
        String refreshed = oAuthService.refreshAccessToken();
        assertEquals("token-refreshed", refreshed);

        // 2 HTTP requests should have been made
        verify(mockHttpClient, times(2)).send(any(), any());
    }

    @Test
    void testGetAccessToken_WhenOAuthFails_ThrowsIllegalStateException() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"error\":\"invalid_grant\",\"error_description\":\"Token expired\"}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(), any());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> oAuthService.getAccessToken());
        assertTrue(ex.getMessage().contains("invalid_grant") || ex.getMessage().contains("400"));
    }

    @Test
    void testGetAccessToken_WhenConfigMissing_ThrowsIllegalStateException() {
        GmailOAuthService unconfigured = new GmailOAuthService(objectMapper, mockHttpClient, null, null, null);
        assertThrows(IllegalStateException.class, unconfigured::getAccessToken);
    }
}
