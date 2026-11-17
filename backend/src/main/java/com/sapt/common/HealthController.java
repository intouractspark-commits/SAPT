package com.sapt.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Lightweight health-check endpoint.
 * Reachable at GET /api/health (no authentication required).
 * Used by Render health checks and for quick manual verification.
 */
@RestController
public class HealthController {

    @GetMapping({"/", "/health"})
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",    "ok",
                "message",   "Backend connected successfully",
                "timestamp", Instant.now().toString()
        ));
    }
}
