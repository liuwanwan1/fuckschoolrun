package com.acooldog.toolbox.sensortest;

public final class SensorStressSample {
    private final long timestampMillis;
    private final double accelerationX;
    private final double accelerationY;
    private final double accelerationZ;
    private final double gyroX;
    private final double gyroY;
    private final double gyroZ;
    private final int stepCount;
    private final double cadenceSpm;
    private final boolean anomaly;
    private final String tag;

    SensorStressSample(
            long timestampMillis,
            double accelerationX,
            double accelerationY,
            double accelerationZ,
            double gyroX,
            double gyroY,
            double gyroZ,
            int stepCount,
            double cadenceSpm,
            boolean anomaly,
            String tag
    ) {
        this.timestampMillis = timestampMillis;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.stepCount = stepCount;
        this.cadenceSpm = cadenceSpm;
        this.anomaly = anomaly;
        this.tag = tag;
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

    public double getCadenceSpm() {
        return cadenceSpm;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public String getTag() {
        return tag;
    }
}
