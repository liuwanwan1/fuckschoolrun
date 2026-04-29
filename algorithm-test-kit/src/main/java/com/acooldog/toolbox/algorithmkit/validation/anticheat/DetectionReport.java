package com.acooldog.toolbox.algorithmkit.validation.anticheat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DetectionReport {
    private final List<String> detectionCapabilities;
    private final double falsePositiveRate;
    private final long detectionLatencyMillis;
    private final List<String> improvementSuggestions;

    public DetectionReport(List<String> detectionCapabilities, double falsePositiveRate, long detectionLatencyMillis, List<String> improvementSuggestions) {
        this.detectionCapabilities = Collections.unmodifiableList(new ArrayList<>(detectionCapabilities));
        this.falsePositiveRate = Math.max(0d, Math.min(1d, falsePositiveRate));
        this.detectionLatencyMillis = Math.max(0L, detectionLatencyMillis);
        this.improvementSuggestions = Collections.unmodifiableList(new ArrayList<>(improvementSuggestions));
    }

    public List<String> getDetectionCapabilities() {
        return detectionCapabilities;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public long getDetectionLatencyMillis() {
        return detectionLatencyMillis;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }
}
