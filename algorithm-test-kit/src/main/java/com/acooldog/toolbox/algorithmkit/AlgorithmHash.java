package com.acooldog.toolbox.algorithmkit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class AlgorithmHash {
    private AlgorithmHash() {
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate SHA-256", exception);
        }
    }
}
