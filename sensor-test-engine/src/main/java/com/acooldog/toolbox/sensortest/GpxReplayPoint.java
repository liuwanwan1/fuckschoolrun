package com.acooldog.toolbox.sensortest;

public final class GpxReplayPoint {
    private final long timestampMillis;
    private final double latitude;
    private final double longitude;
    private final double altitudeMeters;
    private final double speedMetersPerSecond;
    private final double bearingDegrees;

    GpxReplayPoint(
            long timestampMillis,
            double latitude,
            double longitude,
            double altitudeMeters,
            double speedMetersPerSecond,
            double bearingDegrees
    ) {
        this.timestampMillis = timestampMillis;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.bearingDegrees = bearingDegrees;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public double getBearingDegrees() {
        return bearingDegrees;
    }
}
