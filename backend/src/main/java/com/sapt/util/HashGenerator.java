package com.sapt.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: HashGenerator <plaintext-password>");
            System.err.println("SECURITY: Never pass a production password via CLI args on a shared system.");
            System.exit(1);
        }
        String pwd = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("HASH:" + encoder.encode(pwd));
    }
}
