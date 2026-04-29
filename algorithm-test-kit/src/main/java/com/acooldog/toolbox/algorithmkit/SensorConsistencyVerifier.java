package com.acooldog.toolbox.algorithmkit;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class SensorConsistencyVerifier {
    public ConsistencyReport verify(StepSimulationResult stepResult, GpsTrajectoryResult trajectoryResult) {
        if (stepResult == null || trajectoryResult == null) {
            throw new IllegalArgumentException("步频数据和GPS轨迹不能为空");
        }
        int steps = Math.max(1, stepResult.getTotalSteps());
        double distanceMeters = trajectoryResult.getDistanceMeters();
        double strideMeters = distanceMeters / steps;
        double gpsAverageSpeed = distanceMeters / Math.max(1d, trajectoryResult.getDurationSeconds());
        double stepDerivedSpeed = strideMeters * (stepResult.getCadenceSpm() / 60d);

        double strideScore = scoreRange(strideMeters, 0.55d, 1.45d, 0.30d, 2.00d);
        double speedDelta = Math.abs(gpsAverageSpeed - stepDerivedSpeed) / Math.max(0.1d, gpsAverageSpeed);
        double speedScore = Math.max(0d, 1d - speedDelta);
        double durationDelta = Math.abs(stepResult.getDurationSeconds() - trajectoryResult.getDurationSeconds())
                / (double) Math.max(stepResult.getDurationSeconds(), trajectoryResult.getDurationSeconds());
        double durationScore = Math.max(0d, 1d - durationDelta);
        int score = (int) Math.round((strideScore * 45d) + (speedScore * 35d) + (durationScore * 20d));
        String verdict;
        if (score >= 85) {
            verdict = "一致性良好";
        } else if (score >= 65) {
            verdict = "存在轻微不一致";
        } else {
            verdict = "物理一致性不足";
        }
        return new ConsistencyReport(
                Math.max(0, Math.min(100, score)),
                distanceMeters,
                steps,
                strideMeters,
                gpsAverageSpeed,
                stepDerivedSpeed,
                verdict
        );
    }

    private double scoreRange(double value, double idealMin, double idealMax, double hardMin, double hardMax) {
        if (value >= idealMin && value <= idealMax) {
            return 1d;
        }
        if (value < hardMin || value > hardMax) {
            return 0d;
        }
        if (value < idealMin) {
            return (value - hardMin) / Math.max(0.001d, idealMin - hardMin);
        }
        return (hardMax - value) / Math.max(0.001d, hardMax - idealMax);
    }
}
