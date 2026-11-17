package com.sapt.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures the `pending_registrations` table exists in PostgreSQL / Supabase on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingRegistrationTableInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensurePendingRegistrationsTableExists() {
        try {
            log.info("Checking/creating pending_registrations table...");
            String ddl = """
                CREATE TABLE IF NOT EXISTS pending_registrations (
                    id CHAR(36) PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    name VARCHAR(255) NOT NULL,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    college_id CHAR(36),
                    college_name VARCHAR(255),
                    department_id CHAR(36),
                    department_name VARCHAR(255),
                    mentor_id CHAR(36),
                    mentor_name VARCHAR(255),
                    id_card_url TEXT,
                    otp_code VARCHAR(10),
                    otp_expires_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """;
            jdbcTemplate.execute(ddl);

            // Create index if it doesn't already exist
            try {
                jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_pending_email ON pending_registrations(email)");
            } catch (Exception e) {
                log.debug("idx_pending_email index notice: {}", e.getMessage());
            }

            log.info("pending_registrations table verified successfully.");
        } catch (Exception e) {
            log.error("Failed to create pending_registrations table: {}", e.getMessage(), e);
        }
    }
}
