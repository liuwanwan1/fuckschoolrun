package com.acooldog.toolbox.algorithmkit.instruction.validator;

public final class ValidationResult {
    private final boolean accepted;
    private final String message;
    private final long maxDurationMillis;
    private final long memoryLimitBytes;
    private final boolean networkAccess;

    private ValidationResult(boolean accepted, String message, long maxDurationMillis, long memoryLimitBytes, boolean networkAccess) {
        this.accepted = accepted;
        this.message = message;
        this.maxDurationMillis = maxDurationMillis;
        this.memoryLimitBytes = memoryLimitBytes;
        this.networkAccess = networkAccess;
    }

    public static ValidationResult accepted(long maxDurationMillis, long memoryLimitBytes) {
        return new ValidationResult(true, "Accepted", maxDurationMillis, memoryLimitBytes, false);
    }

    public static ValidationResult rejected(String message) {
        return new ValidationResult(false, message, 0L, 0L, false);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getMessage() {
        return message;
    }

    public long getMaxDurationMillis() {
        return maxDurationMillis;
    }

    public long getMemoryLimitBytes() {
        return memoryLimitBytes;
    }

    public boolean isNetworkAccess() {
        return networkAccess;
    }
}
