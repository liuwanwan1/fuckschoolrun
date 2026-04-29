package com.acooldog.toolbox.algorithmkit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class StepSimulationResult {
    private final int cadenceSpm;
    private final int durationSeconds;
    private final int sampleRateHz;
    private final int totalSteps;
    private final List<StepSample> samples;

    StepSimulationResult(int cadenceSpm, int durationSeconds, int sampleRateHz, int totalSteps, List<StepSample> samples) {
        this.cadenceSpm = cadenceSpm;
        this.durationSeconds = durationSeconds;
        this.sampleRateHz = sampleRateHz;
        this.totalSteps = totalSteps;
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
    }

    public int getCadenceSpm() {
        return cadenceSpm;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public List<StepSample> getSamples() {
        return samples;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"type\":\"step_cadence\",\"cadenceSpm\":").append(cadenceSpm)
                .append(",\"durationSeconds\":").append(durationSeconds)
                .append(",\"sampleRateHz\":").append(sampleRateHz)
                .append(",\"totalSteps\":").append(totalSteps)
                .append(",\"samples\":[");
        for (int index = 0; index < samples.size(); index++) {
            StepSample sample = samples.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(
                    Locale.US,
                    "{\"t\":%d,\"ax\":%.4f,\"ay\":%.4f,\"az\":%.4f,\"gx\":%.5f,\"gy\":%.5f,\"gz\":%.5f,\"steps\":%d}",
                    sample.getTimestampMillis(),
                    sample.getAccelerationX(),
                    sample.getAccelerationY(),
                    sample.getAccelerationZ(),
                    sample.getGyroX(),
                    sample.getGyroY(),
                    sample.getGyroZ(),
                    sample.getStepCount()
            ));
        }
        builder.append("]}");
        return builder.toString();
    }

    public String toCsv() {
        StringBuilder builder = new StringBuilder("timestamp_ms,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,step_count\n");
        for (StepSample sample : samples) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%d\n",
                    sample.getTimestampMillis(),
                    sample.getAccelerationX(),
                    sample.getAccelerationY(),
                    sample.getAccelerationZ(),
                    sample.getGyroX(),
                    sample.getGyroY(),
                    sample.getGyroZ(),
                    sample.getStepCount()
            ));
        }
        return builder.toString();
    }

    public String outputSummary() {
        return "数据点=" + samples.size() + ", 步数=" + totalSteps;
    }
}
