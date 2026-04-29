package com.acooldog.toolbox.algorithmkit.security.audit;

public final class VerificationResult {
    private final boolean valid;
    private final String message;

    private VerificationResult(boolean valid, String message) {
        this.valid = valid;
        this.message = message;
    }

    public static VerificationResult valid(String message) {
        return new VerificationResult(true, message);
    }

    public static VerificationResult invalid(String message) {
        return new VerificationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }
}
