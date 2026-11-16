package com.sapt;

import com.sapt.util.SecretMasker;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * ============================================================
 * DebugEnv - Diagnostic utility for local development ONLY.
 * ============================================================
 * Run this class manually from your IDE or CLI to verify that
 * the .env file is being loaded correctly.
 *
 * SECURITY: Sensitive values (passwords, secrets, URIs) are
 * masked using SecretMasker — raw values are NEVER printed.
 *
 * Usage: mvn exec:java -Dexec.mainClass="com.sapt.DebugEnv"
 * ============================================================
 */
public class DebugEnv {
    public static void main(String[] args) {
        System.out.println("=== SAPT - ENV DIAGNOSTIC (masked) ===");
        System.out.println("user.dir: " + System.getProperty("user.dir"));
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();
            System.out.println("Dotenv entries loaded: " + dotenv.entries().size());
            dotenv.entries().forEach(entry ->
                System.out.println(entry.getKey() + " = "
                    + SecretMasker.maskEnvEntry(entry.getKey(), entry.getValue()))
            );
        } catch (Exception e) {
            System.err.println("Error loading dotenv: " + e.getMessage());
        }
        System.out.println("=== END DIAGNOSTIC ===");
    }
}
