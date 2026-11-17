package com.sapt;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * SAPT - Student Activity Point Tracker
 * Main Application Entry Point
 * ============================================================
 *
 * This is the root Spring Boot application class.
 * Do NOT modify this file unless you know what you are doing.
 *
 * Author: SAPT Dev Team
 *
 * SECURITY NOTE:
 *   The static block previously logged ALL .env entries (including
 *   MONGO_URI, MAIL_PASSWORD, JWT_SECRET, PG_PASSWORD) to stderr
 *   via System.err.println. That code has been removed.
 *   Environment variables are NEVER logged — only the count is printed.
 * ============================================================
 */
@SpringBootApplication
public class SaptApplication {

    static {
        // Load .env into System properties so Spring @Value and ${...} work.
        // SECURITY: We intentionally do NOT log any key names or values.
        Dotenv dotenv = Dotenv.configure()
                .directory("./")       // looks for .env in backend/ root
                .ignoreIfMissing()     // don't crash if .env is absent
                .load();

        dotenv.entries().forEach(entry -> {
            // Only inject if not already set by the OS environment (OS wins)
            if (System.getProperty(entry.getKey()) == null
                    && System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        // Safe: logs only the count, never keys or values
        System.out.printf("[SAPT] Dotenv: loaded %d environment entr%s.%n",
                dotenv.entries().size(),
                dotenv.entries().size() == 1 ? "y" : "ies");

        // Ensure Java JNDI uses reliable DNS for mongodb+srv:// resolution on Windows
        if (System.getProperty("java.naming.provider.url") == null) {
            System.setProperty("java.naming.provider.url", "dns://8.8.8.8");
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(SaptApplication.class, args);
    }
}
