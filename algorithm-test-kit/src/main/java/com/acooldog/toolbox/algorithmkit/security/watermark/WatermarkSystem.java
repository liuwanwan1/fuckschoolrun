package com.acooldog.toolbox.algorithmkit.security.watermark;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class WatermarkSystem {
    private static final String MARKER = "\n__ALGORITHM_TEST_WATERMARK__:";

    public byte[] embedWatermark(byte[] data, WatermarkMetadata metadata) {
        byte[] source = data == null ? new byte[0] : data;
        String payload = metadata.encode();
        String signature = AlgorithmHash.sha256(payload);
        String encoded = Base64.getEncoder().encodeToString((payload + "|" + signature).getBytes(StandardCharsets.UTF_8));
        byte[] suffix = (MARKER + encoded).getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[source.length + suffix.length];
        System.arraycopy(source, 0, result, 0, source.length);
        System.arraycopy(suffix, 0, result, source.length, suffix.length);
        return result;
    }

    public WatermarkResult extractAndVerify(byte[] data) {
        String text = new String(data == null ? new byte[0] : data, StandardCharsets.UTF_8);
        int markerIndex = text.lastIndexOf(MARKER);
        if (markerIndex < 0) {
            return new WatermarkResult(WatermarkResult.Status.NO_WATERMARK, null);
        }
        try {
            String encoded = text.substring(markerIndex + MARKER.length()).trim();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", -1);
            if (parts.length < 5) {
                return new WatermarkResult(WatermarkResult.Status.TAMPERED, null);
            }
            String payload = parts[0] + "|" + parts[1] + "|" + parts[2] + "|" + parts[3];
            if (!AlgorithmHash.sha256(payload).equals(parts[4])) {
                return new WatermarkResult(WatermarkResult.Status.TAMPERED, null);
            }
            WatermarkMetadata metadata = WatermarkMetadata.decode(payload);
            if (metadata == null) {
                return new WatermarkResult(WatermarkResult.Status.TAMPERED, null);
            }
            if (metadata.expired()) {
                return new WatermarkResult(WatermarkResult.Status.EXPIRED, metadata);
            }
            return new WatermarkResult(WatermarkResult.Status.VALID, metadata);
        } catch (Exception ignored) {
            return new WatermarkResult(WatermarkResult.Status.TAMPERED, null);
        }
    }
}
