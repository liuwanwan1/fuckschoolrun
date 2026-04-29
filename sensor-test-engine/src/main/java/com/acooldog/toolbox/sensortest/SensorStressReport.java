package com.acooldog.toolbox.sensortest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class SensorStressReport {
    public static final String WATERMARK = "FOR TESTING ONLY";

    private final SensorStressConfig config;
    private final List<SensorStressSample> samples;
    private final List<RiskFinding> findings;
    private final int totalSteps;
    private final double cadenceVariance;
    private final int anomalySamples;
    private final int riskScore;

    SensorStressReport(
            SensorStressConfig config,
            List<SensorStressSample> samples,
            List<RiskFinding> findings,
            int totalSteps,
            double cadenceVariance,
            int anomalySamples,
            int riskScore
    ) {
        this.config = config;
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
        this.totalSteps = totalSteps;
        this.cadenceVariance = cadenceVariance;
        this.anomalySamples = anomalySamples;
        this.riskScore = riskScore;
    }

    public SensorStressConfig getConfig() {
        return config;
    }

    public List<SensorStressSample> getSamples() {
        return samples;
    }

    public List<RiskFinding> getFindings() {
        return findings;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public double getCadenceVariance() {
        return cadenceVariance;
    }

    public int getAnomalySamples() {
        return anomalySamples;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String summary() {
        return String.format(
                Locale.US,
                "%s | mode=%s, samples=%d, steps=%d, riskScore=%d, findings=%d",
                WATERMARK,
                config.getMode(),
                samples.size(),
                totalSteps,
                riskScore,
                findings.size()
        );
    }

    public String toHumanReport() {
        StringBuilder builder = new StringBuilder();
        builder.append(WATERMARK).append('\n');
        builder.append("Sensor pressure report\n");
        builder.append(String.format(
                Locale.US,
                "mode=%s, targetCadence=%d spm, duration=%d s, sampleRate=%d Hz\n",
                config.getMode(),
                config.getTargetCadenceSpm(),
                config.getDurationSeconds(),
                config.getSampleRateHz()
        ));
        builder.append(String.format(
                Locale.US,
                "samples=%d, totalSteps=%d, cadenceVariance=%.3f, anomalySamples=%d, riskScore=%d\n",
                samples.size(),
                totalSteps,
                cadenceVariance,
                anomalySamples,
                riskScore
        ));
        for (RiskFinding finding : findings) {
            builder.append("- [")
                    .append(finding.getSeverity())
                    .append("] ")
                    .append(finding.getCategory())
                    .append(": ")
                    .append(finding.getDescription())
                    .append(" | expectedDefense=")
                    .append(finding.getExpectedDefenseSignal())
                    .append('\n');
        }
        return builder.toString();
    }

    public String toCsv() {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(WATERMARK).append('\n');
        builder.append("timestamp_ms,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,step_count,cadence_spm,anomaly,tag\n");
        for (SensorStressSample sample : samples) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%.4f,%.4f,%.4f,%.5f,%.5f,%.5f,%d,%.2f,%s,%s\n",
                    sample.getTimestampMillis(),
                    sample.getAccelerationX(),
                    sample.getAccelerationY(),
                    sample.getAccelerationZ(),
                    sample.getGyroX(),
                    sample.getGyroY(),
                    sample.getGyroZ(),
                    sample.getStepCount(),
                    sample.getCadenceSpm(),
                    sample.isAnomaly(),
                    escapeCsv(sample.getTag())
            ));
        }
        return builder.toString();
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"watermark\":\"").append(WATERMARK).append("\",")
                .append("\"type\":\"sensor_pressure_report\",")
                .append("\"mode\":\"").append(config.getMode()).append("\",")
                .append("\"targetCadenceSpm\":").append(config.getTargetCadenceSpm()).append(',')
                .append("\"durationSeconds\":").append(config.getDurationSeconds()).append(',')
                .append("\"sampleRateHz\":").append(config.getSampleRateHz()).append(',')
                .append("\"totalSteps\":").append(totalSteps).append(',')
                .append("\"cadenceVariance\":").append(String.format(Locale.US, "%.5f", cadenceVariance)).append(',')
                .append("\"anomalySamples\":").append(anomalySamples).append(',')
                .append("\"riskScore\":").append(riskScore).append(',')
                .append("\"findings\":[");
        for (int index = 0; index < findings.size(); index++) {
            RiskFinding finding = findings.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"category\":\"").append(json(finding.getCategory()))
                    .append("\",\"severity\":\"").append(json(finding.getSeverity()))
                    .append("\",\"description\":\"").append(json(finding.getDescription()))
                    .append("\",\"expectedDefense\":\"").append(json(finding.getExpectedDefenseSignal()))
                    .append("\"}");
        }
        builder.append("],\"samples\":[");
        for (int index = 0; index < samples.size(); index++) {
            SensorStressSample sample = samples.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(
                    Locale.US,
                    "{\"t\":%d,\"ax\":%.4f,\"ay\":%.4f,\"az\":%.4f,\"gx\":%.5f,\"gy\":%.5f,\"gz\":%.5f,\"steps\":%d,\"cadence\":%.2f,\"anomaly\":%s,\"tag\":\"%s\"}",
                    sample.getTimestampMillis(),
                    sample.getAccelerationX(),
                    sample.getAccelerationY(),
                    sample.getAccelerationZ(),
                    sample.getGyroX(),
                    sample.getGyroY(),
                    sample.getGyroZ(),
                    sample.getStepCount(),
                    sample.getCadenceSpm(),
                    sample.isAnomaly(),
                    json(sample.getTag())
            ));
        }
        builder.append("]}");
        return builder.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
