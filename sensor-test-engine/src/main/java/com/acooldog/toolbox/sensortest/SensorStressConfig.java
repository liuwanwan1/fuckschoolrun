package com.acooldog.toolbox.sensortest;

import java.util.Objects;

public final class SensorStressConfig {
    public static final int MIN_CADENCE_SPM = 90;
    public static final int MAX_CADENCE_SPM = 260;
    public static final int DEFAULT_SAMPLE_RATE_HZ = 20;

    private final SensorStressMode mode;
    private final int targetCadenceSpm;
    private final int durationSeconds;
    private final int sampleRateHz;
    private final long randomSeed;

    private SensorStressConfig(
            SensorStressMode mode,
            int targetCadenceSpm,
            int durationSeconds,
            int sampleRateHz,
            long randomSeed
    ) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.targetCadenceSpm = targetCadenceSpm;
        this.durationSeconds = durationSeconds;
        this.sampleRateHz = sampleRateHz;
        this.randomSeed = randomSeed;
        validate();
    }

    public static SensorStressConfig create(
            SensorStressMode mode,
            int targetCadenceSpm,
            int durationSeconds,
            long randomSeed
    ) {
        return new SensorStressConfig(mode, targetCadenceSpm, durationSeconds, DEFAULT_SAMPLE_RATE_HZ, randomSeed);
    }

    public SensorStressMode getMode() {
        return mode;
    }

    public int getTargetCadenceSpm() {
        return targetCadenceSpm;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    private void validate() {
        if (targetCadenceSpm < MIN_CADENCE_SPM || targetCadenceSpm > MAX_CADENCE_SPM) {
            throw new IllegalArgumentException("targetCadenceSpm must be between 90 and 260");
        }
        if (durationSeconds < 5 || durationSeconds > 1800) {
            throw new IllegalArgumentException("durationSeconds must be between 5 and 1800");
        }
        if (sampleRateHz < 5 || sampleRateHz > 100) {
            throw new IllegalArgumentException("sampleRateHz must be between 5 and 100");
        }
    }
}
