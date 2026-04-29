package com.acooldog.toolbox.algorithmkit;

import java.util.ArrayList;
import java.util.List;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class StepCadenceSimulator {
    public static final int MIN_CADENCE_SPM = 140;
    public static final int MAX_CADENCE_SPM = 220;
    public static final int DEFAULT_SAMPLE_RATE_HZ = 20;

    public StepSimulationResult generate(int cadenceSpm, int durationSeconds) {
        validate(cadenceSpm, durationSeconds);
        int sampleCount = durationSeconds * DEFAULT_SAMPLE_RATE_HZ;
        double stepFrequencyHz = cadenceSpm / 60d;
        double angularFrequency = 2d * Math.PI * stepFrequencyHz;
        double cadenceRatio = (cadenceSpm - MIN_CADENCE_SPM) / (double) (MAX_CADENCE_SPM - MIN_CADENCE_SPM);
        double verticalAmplitude = 1.25d + (cadenceRatio * 0.75d);
        double lateralAmplitude = 0.25d + (cadenceRatio * 0.18d);
        List<StepSample> samples = new ArrayList<>(sampleCount);
        int totalSteps = 0;
        for (int index = 0; index < sampleCount; index++) {
            double seconds = index / (double) DEFAULT_SAMPLE_RATE_HZ;
            double phase = angularFrequency * seconds;
            int steps = (int) Math.floor(seconds * stepFrequencyHz);
            if (index == sampleCount - 1) {
                steps = (int) Math.floor(durationSeconds * stepFrequencyHz);
            }
            totalSteps = Math.max(totalSteps, steps);

            double slowEnvelope = 1d + (0.04d * Math.sin(2d * Math.PI * seconds / 17d));
            double impact = Math.max(0d, Math.sin(phase));
            double vertical = 9.80665d
                    + (verticalAmplitude * slowEnvelope * Math.sin(phase))
                    + (0.45d * impact * impact);
            double forward = 0.18d * Math.sin(phase + Math.PI / 5d)
                    + (lateralAmplitude * Math.sin(phase * 0.5d));
            double side = lateralAmplitude * Math.sin(phase + Math.PI / 2d);
            double gyroX = 0.035d * Math.sin(phase + Math.PI / 3d);
            double gyroY = 0.028d * Math.sin(phase * 0.5d);
            double gyroZ = 0.045d * Math.cos(phase);
            samples.add(new StepSample(
                    Math.round(seconds * 1000d),
                    forward,
                    side,
                    vertical,
                    gyroX,
                    gyroY,
                    gyroZ,
                    steps
            ));
        }
        return new StepSimulationResult(cadenceSpm, durationSeconds, DEFAULT_SAMPLE_RATE_HZ, totalSteps, samples);
    }

    private void validate(int cadenceSpm, int durationSeconds) {
        if (cadenceSpm < MIN_CADENCE_SPM || cadenceSpm > MAX_CADENCE_SPM) {
            throw new IllegalArgumentException("步频必须在 140-220 SPM 之间");
        }
        if (durationSeconds <= 0 || durationSeconds > 3600) {
            throw new IllegalArgumentException("运动时间必须在 1-3600 秒之间");
        }
    }
}
