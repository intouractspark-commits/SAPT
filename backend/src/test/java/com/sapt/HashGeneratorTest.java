package com.sapt;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * SECURITY NOTE: This test uses a dummy test-only password.
 * Never use a real/production password in test code.
 */
public class HashGeneratorTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // SECURITY: Use a test-only dummy value — never a real production password.
        String hash = encoder.encode("test-only-dummy-password-do-not-use");
        System.out.println("=== BCRYPT HASH ===");
        System.out.println(hash);
        System.out.println("===================");
    }
}
