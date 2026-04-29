package com.acooldog.toolbox.algorithmkit.security.watermark;

public final class WatermarkResult {
    public enum Status {
        NO_WATERMARK,
        TAMPERED,
        EXPIRED,
        VALID
    }

    private final Status status;
    private final WatermarkMetadata metadata;

    public WatermarkResult(Status status, WatermarkMetadata metadata) {
        this.status = status;
        this.metadata = metadata;
    }

    public Status getStatus() {
        return status;
    }

    public WatermarkMetadata getMetadata() {
        return metadata;
    }
}
