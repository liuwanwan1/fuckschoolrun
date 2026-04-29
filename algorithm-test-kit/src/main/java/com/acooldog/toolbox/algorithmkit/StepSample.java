package com.acooldog.toolbox.algorithmkit;

public final class StepSample {
    private final long timestampMillis;
    private final double accelerationX;
    private final double accelerationY;
    private final double accelerationZ;
    private final double gyroX;
    private final double gyroY;
    private final double gyroZ;
    private final int stepCount;

    public StepSample(
            long timestampMillis,
            double accelerationX,
            double accelerationY,
            double accelerationZ,
            double gyroX,
            double gyroY,
            double gyroZ,
            int stepCount
    ) {
        this.timestampMillis = timestampMillis;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.stepCount = stepCount;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public double getAccelerationX() {
        return accelerationX;
    }

    public double getAccelerationY() {
        return accelerationY;
    }

    public double getAccelerationZ() {
        return accelerationZ;
    }

    public double getGyroX() {
        return gyroX;
    }

    public double getGyroY() {
        return gyroY;
    }

    public double getGyroZ() {
        return gyroZ;
    }

    public int getStepCount() {
        return stepCount;
    }
}
