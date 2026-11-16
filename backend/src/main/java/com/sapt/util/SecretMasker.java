package com.sapt.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================
 * SecretMasker - Secure Credential Masking Utility
 * ============================================================
 * Masks sensitive values so they can be safely referenced in
 * logs without exposing credentials.
 *
 * Usage:
 *   log.info("MONGO_URI = {}", SecretMasker.maskMongoUri(uri));
 *   log.info("MAIL_PASSWORD = {}", SecretMasker.maskSecret(password));
 *   log.info("JWT_SECRET = {}", SecretMasker.maskSecret(secret));
 *
 * Rules:
 *  - Values shorter than 4 characters → fully redacted: [REDACTED]
 *  - Values 4–8 characters             → show last 3: ****xyz
 *  - Values > 8 characters             → show last 4: ****abcd
 *  - MongoDB URIs                       → show scheme + host only
 *  - JDBC URLs                          → strip password= parameter
 * ============================================================
 */
public final class SecretMasker {

    private static final Pattern MONGO_CREDENTIALS =
            Pattern.compile("(mongodb(?:\\+srv)?://)([^@]+)@(.+)");

    private static final Pattern JDBC_PASSWORD =
            Pattern.compile("(?i)(password=)[^;&]+");

    // Prevent instantiation
    private SecretMasker() {}

    /**
     * Generic masker: hides all but the last 3–4 chars.
     * Returns [REDACTED] for null / very short values.
     */
    public static String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "[REDACTED]";
        }
        int len = value.length();
        if (len < 4) {
            return "[REDACTED]";
        }
        int show = len <= 8 ? 3 : 4;
        return "****" + value.substring(len - show);
    }

    /**
     * Masks a MongoDB URI, keeping only the scheme and host.
     *
     * Input:  mongodb+srv://user:pass@cluster0.example.net/db?opts
     * Output: mongodb+srv://****@cluster0.example.net
     */
    public static String maskMongoUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "[REDACTED]";
        }
        Matcher m = MONGO_CREDENTIALS.matcher(uri);
        if (m.find()) {
            String scheme = m.group(1);
            String hostAndRest = m.group(3);
            // Strip path and query string from host
            String host = hostAndRest.split("[/?]")[0];
            return scheme + "****@" + host;
        }
        // If no credentials in URI, still hide the whole thing
        return "[REDACTED-URI]";
    }

    /**
     * Masks a JDBC URL, redacting the password= query param if present.
     *
     * Input:  jdbc:postgresql://host:5432/db?password=secret&ssl=true
     * Output: jdbc:postgresql://host:5432/db?password=****&ssl=true
     */
    public static String maskJdbcUrl(String url) {
        if (url == null || url.isBlank()) {
            return "[REDACTED]";
        }
        return JDBC_PASSWORD.matcher(url).replaceAll("$1****");
    }

    /**
     * Masks all entries from a dotenv dump.
     * Any key containing a known sensitive keyword is fully masked.
     */
    public static String maskEnvEntry(String key, String value) {
        if (key == null) return "[REDACTED]";
        String upper = key.toUpperCase();
        if (upper.contains("PASSWORD") || upper.contains("SECRET")
                || upper.contains("TOKEN")  || upper.contains("KEY")
                || upper.contains("URI")    || upper.contains("URL")
                || upper.contains("CREDENTIAL") || upper.contains("DSN")) {

            if (upper.contains("MONGO") || upper.contains("URI")) {
                return maskMongoUri(value);
            }
            if (upper.contains("URL") || upper.contains("JDBC")) {
                return maskJdbcUrl(value);
            }
            return maskSecret(value);
        }
        // Non-sensitive keys are safe to log as-is
        return value;
    }
}
