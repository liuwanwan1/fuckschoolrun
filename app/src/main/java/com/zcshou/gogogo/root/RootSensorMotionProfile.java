package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RootSensorMotionProfile {
    static final String KEY_SENSOR_JITTER_RANGE = "sensorNaturalJitterRange";
    static final String KEY_SENSOR_JITTER_PROBABILITY = "sensorNaturalJitterProbability";
    static final String KEY_SENSOR_WAVEFORM = "sensorMotionWaveform";

    private static final double MIN_JITTER_RANGE = 0d;
    private static final double MAX_JITTER_RANGE = 3d;
    private static final double MIN_JITTER_PROBABILITY = 0d;
    private static final double MAX_JITTER_PROBABILITY = 1d;
    private static final int MIN_WAVEFORM_SAMPLES = 8;
    private static final int MAX_WAVEFORM_SAMPLES = 96;

    private final double naturalJitterRange;
    private final double naturalJitterProbability;
    private final List<Double> waveformSamples;

    public RootSensorMotionProfile(
            double naturalJitterRange,
            double naturalJitterProbability,
            @NonNull List<Double> waveformSamples
    ) {
        this.naturalJitterRange = clamp(naturalJitterRange, MIN_JITTER_RANGE, MAX_JITTER_RANGE);
        this.naturalJitterProbability = clamp(naturalJitterProbability, MIN_JITTER_PROBABILITY, MAX_JITTER_PROBABILITY);
        this.waveformSamples = Collections.unmodifiableList(sanitizeWaveform(waveformSamples));
    }

    @NonNull
    public static RootSensorMotionProfile defaults() {
        List<Double> samples = new ArrayList<>();
        for (int index = 0; index < 32; index++) {
            double phase = index * Math.PI * 2d / 32d;
            samples.add(Math.sin(phase));
        }
        return new RootSensorMotionProfile(0.28d, 0.35d, samples);
    }

    @NonNull
    public static RootSensorMotionProfile fromJson(
            @Nullable JSONObject object,
            @NonNull RootSensorMotionProfile fallback
    ) {
        if (object == null) {
            return fallback;
        }
        return new RootSensorMotionProfile(
                object.optDouble(KEY_SENSOR_JITTER_RANGE, fallback.getNaturalJitterRange()),
                object.optDouble(KEY_SENSOR_JITTER_PROBABILITY, fallback.getNaturalJitterProbability()),
                parseWaveform(object.optString(KEY_SENSOR_WAVEFORM, encodeWaveform(fallback.getWaveformSamples())),
                        fallback.getWaveformSamples())
        );
    }

    public void writeToJson(@NonNull JSONObject object) {
        try {
            object.put(KEY_SENSOR_JITTER_RANGE, naturalJitterRange);
            object.put(KEY_SENSOR_JITTER_PROBABILITY, naturalJitterProbability);
            object.put(KEY_SENSOR_WAVEFORM, encodeWaveform(waveformSamples));
        } catch (Exception ignored) {
            // Keep best-effort persistence aligned with RootDiagnosticSettings.
        }
    }

    @NonNull
    public static List<Double> normalizeRecordedMagnitudes(@NonNull List<Double> rawMagnitudes, int targetCount) {
        if (rawMagnitudes.isEmpty()) {
            return defaults().getWaveformSamples();
        }
        int count = clamp(targetCount, MIN_WAVEFORM_SAMPLES, MAX_WAVEFORM_SAMPLES);
        List<Double> centered = new ArrayList<>();
        double average = 0d;
        for (double value : rawMagnitudes) {
            average += value;
        }
        average /= rawMagnitudes.size();
        double maxAbs = 0d;
        for (double value : rawMagnitudes) {
            double centeredValue = value - average;
            centered.add(centeredValue);
            maxAbs = Math.max(maxAbs, Math.abs(centeredValue));
        }
        if (maxAbs < 0.0001d) {
            return defaults().getWaveformSamples();
        }
        List<Double> normalized = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            double position = index * (centered.size() - 1d) / Math.max(1d, count - 1d);
            int left = (int) Math.floor(position);
            int right = Math.min(centered.size() - 1, left + 1);
            double ratio = position - left;
            double interpolated = centered.get(left) * (1d - ratio) + centered.get(right) * ratio;
            normalized.add(clamp(interpolated / maxAbs, -1d, 1d));
        }
        return normalized;
    }

    public double getNaturalJitterRange() {
        return naturalJitterRange;
    }

    public double getNaturalJitterProbability() {
        return naturalJitterProbability;
    }

    @NonNull
    public List<Double> getWaveformSamples() {
        return waveformSamples;
    }

    @NonNull
    public String summarize() {
        return String.format(
                Locale.getDefault(),
                "jitter=%.2f@%.0f%%, waveform=%d",
                naturalJitterRange,
                naturalJitterProbability * 100d,
                waveformSamples.size()
        );
    }

    @NonNull
    private static List<Double> parseWaveform(@Nullable String raw, @NonNull List<Double> fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        String[] parts = raw.split(",");
        List<Double> values = new ArrayList<>();
        for (String part : parts) {
            try {
                values.add(Double.parseDouble(part.trim()));
            } catch (Exception ignored) {
                // Skip invalid persisted samples.
            }
        }
        return values.isEmpty() ? fallback : values;
    }

    @NonNull
    private static String encodeWaveform(@NonNull List<Double> samples) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < samples.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.4f", samples.get(index)));
        }
        return builder.toString();
    }

    @NonNull
    private static List<Double> sanitizeWaveform(@NonNull List<Double> samples) {
        List<Double> sanitized = new ArrayList<>();
        for (Double sample : samples) {
            double value = sample == null ? 0d : sample;
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                value = 0d;
            }
            sanitized.add(clamp(value, -1d, 1d));
            if (sanitized.size() >= MAX_WAVEFORM_SAMPLES) {
                break;
            }
        }
        if (sanitized.size() >= MIN_WAVEFORM_SAMPLES) {
            return sanitized;
        }
        return new ArrayList<>(defaults().getWaveformSamples());
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
