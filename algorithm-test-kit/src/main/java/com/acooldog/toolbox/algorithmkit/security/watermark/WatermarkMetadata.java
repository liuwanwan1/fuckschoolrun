package com.acooldog.toolbox.algorithmkit.security.watermark;

public final class WatermarkMetadata {
    private final String operator;
    private final String scenarioId;
    private final long createdAt;
    private final long expiresAt;

    public WatermarkMetadata(String operator, String scenarioId, long createdAt, long expiresAt) {
        this.operator = operator == null ? "" : operator;
        this.scenarioId = scenarioId == null ? "" : scenarioId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getOperator() {
        return operator;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean expired() {
        return expiresAt > 0L && System.currentTimeMillis() > expiresAt;
    }

    public String encode() {
        return operator + "|" + scenarioId + "|" + createdAt + "|" + expiresAt;
    }

    public static WatermarkMetadata decode(String raw) {
        String[] parts = raw == null ? new String[0] : raw.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }
        return new WatermarkMetadata(parts[0], parts[1], parseLong(parts[2]), parseLong(parts[3]));
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
