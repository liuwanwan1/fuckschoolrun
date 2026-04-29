package com.acooldog.toolbox.sensortest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SensorStressEngine {
    private static final double GRAVITY = 9.80665d;

    public SensorStressReport generate(SensorStressConfig config) {
        Random random = new Random(config.getRandomSeed());
        int sampleCount = config.getDurationSeconds() * config.getSampleRateHz();
        List<SensorStressSample> samples = new ArrayList<>(sampleCount);
        double stepAccumulator = 0d;
        double phase = 0d;
        double cadenceSum = 0d;
        double cadenceSquareSum = 0d;
        int anomalySamples = 0;

        for (int index = 0; index < sampleCount; index++) {
            double seconds = index / (double) config.getSampleRateHz();
            CadencePoint cadencePoint = cadenceAt(config, seconds, random);
            double cadence = cadencePoint.cadenceSpm;
            double frequencyHz = cadence / 60d;
            stepAccumulator += frequencyHz / config.getSampleRateHz();
            phase += 2d * Math.PI * frequencyHz / config.getSampleRateHz();
            cadenceSum += cadence;
            cadenceSquareSum += cadence * cadence;

            double cadenceRatio = clamp((cadence - SensorStressConfig.MIN_CADENCE_SPM)
                    / (double) (SensorStressConfig.MAX_CADENCE_SPM - SensorStressConfig.MIN_CADENCE_SPM), 0d, 1d);
            double verticalAmplitude = 1.15d + (cadenceRatio * 1.05d);
            double lateralAmplitude = 0.20d + (cadenceRatio * 0.28d);
            double impact = Math.max(0d, Math.sin(phase));
            double anomalyBoost = cadencePoint.anomaly ? 2.8d : 1d;
            double vertical = GRAVITY
                    + (verticalAmplitude * anomalyBoost * Math.sin(phase))
                    + (0.45d * anomalyBoost * impact * impact);
            double forward = (0.18d + lateralAmplitude) * Math.sin(phase + Math.PI / 5d);
            double side = lateralAmplitude * Math.sin(phase + Math.PI / 2d);
            double gyroX = 0.035d * anomalyBoost * Math.sin(phase + Math.PI / 3d);
            double gyroY = 0.028d * Math.sin(phase * 0.5d);
            double gyroZ = 0.045d * anomalyBoost * Math.cos(phase);
            if (cadencePoint.anomaly) {
                anomalySamples++;
            }
            samples.add(new SensorStressSample(
                    Math.round(seconds * 1000d),
                    forward,
                    side,
                    vertical,
                    gyroX,
                    gyroY,
                    gyroZ,
                    (int) Math.floor(stepAccumulator),
                    cadence,
                    cadencePoint.anomaly,
                    cadencePoint.tag
            ));
        }

        int totalSteps = samples.isEmpty() ? 0 : samples.get(samples.size() - 1).getStepCount();
        double mean = cadenceSum / Math.max(1, samples.size());
        double cadenceVariance = (cadenceSquareSum / Math.max(1, samples.size())) - (mean * mean);
        List<RiskFinding> findings = findingsFor(config, cadenceVariance, anomalySamples);
        int riskScore = Math.min(100, (findings.size() * 22) + (anomalySamples > 0 ? 18 : 0));
        return new SensorStressReport(config, samples, findings, totalSteps, cadenceVariance, anomalySamples, riskScore);
    }

    private CadencePoint cadenceAt(SensorStressConfig config, double seconds, Random random) {
        double base = config.getTargetCadenceSpm();
        if (config.getMode() == SensorStressMode.CONSTANT_CADENCE) {
            return new CadencePoint(base, false, "constant");
        }
        if (config.getMode() == SensorStressMode.RANDOM_FLUCTUATION) {
            double slowWave = 7.5d * Math.sin((2d * Math.PI * seconds) / 23d);
            double jitter = (random.nextDouble() - 0.5d) * 8d;
            return new CadencePoint(clamp(base + slowWave + jitter, 90d, 260d), false, "random_fluctuation");
        }
        double spikeStart = config.getDurationSeconds() * 0.48d;
        double spikeEnd = Math.min(config.getDurationSeconds() - 1d, spikeStart + Math.max(3d, config.getDurationSeconds() * 0.08d));
        if (seconds >= spikeStart && seconds <= spikeEnd) {
            double spikeCadence = Math.min(260d, base * 1.65d);
            return new CadencePoint(spikeCadence, true, "cadence_spike");
        }
        double recoveryWave = 5d * Math.sin((2d * Math.PI * seconds) / 19d);
        return new CadencePoint(clamp(base + recoveryWave, 90d, 260d), false, "spike_baseline");
    }

    private List<RiskFinding> findingsFor(SensorStressConfig config, double cadenceVariance, int anomalySamples) {
        List<RiskFinding> findings = new ArrayList<>();
        if (config.getMode() == SensorStressMode.CONSTANT_CADENCE) {
            findings.add(new RiskFinding(
                    "cadence_uniformity",
                    "medium",
                    "Cadence stays mathematically constant across the full window.",
                    "low entropy cadence or overly stable step interval alert"
            ));
        }
        if (config.getMode() == SensorStressMode.RANDOM_FLUCTUATION) {
            findings.add(new RiskFinding(
                    "cadence_noise_model",
                    "low",
                    "Cadence contains pseudo-random fluctuation around a controlled target.",
                    "distribution and autocorrelation check should separate natural drift from generated noise"
            ));
        }
        if (config.getMode() == SensorStressMode.SPIKE_ANOMALY || anomalySamples > 0) {
            findings.add(new RiskFinding(
                    "step_cadence_spike",
                    "high",
                    "Cadence and inertial amplitude jump abruptly inside a short time window.",
                    "sudden acceleration and step-frequency discontinuity alert"
            ));
        }
        if (cadenceVariance < 0.01d) {
            findings.add(new RiskFinding(
                    "variance_floor",
                    "medium",
                    "Cadence variance is near zero after sampling.",
                    "minimum biological variance threshold"
            ));
        }
        return findings;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class CadencePoint {
        private final double cadenceSpm;
        private final boolean anomaly;
        private final String tag;

        private CadencePoint(double cadenceSpm, boolean anomaly, String tag) {
            this.cadenceSpm = cadenceSpm;
            this.anomaly = anomaly;
            this.tag = tag;
        }
    }
}
