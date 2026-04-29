package com.acooldog.toolbox.root;

public final class RootGmSamplePoint {
    private final long timestampOffsetMillis;
    private final double distanceMeters;
    private final double latitude;
    private final double longitude;
    private final double altitudeMeters;
    private final double speedMetersPerSecond;

    RootGmSamplePoint(
            long timestampOffsetMillis,
            double distanceMeters,
            double latitude,
            double longitude,
            double altitudeMeters,
            double speedMetersPerSecond
    ) {
        this.timestampOffsetMillis = timestampOffsetMillis;
        this.distanceMeters = distanceMeters;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.speedMetersPerSecond = speedMetersPerSecond;
    }

    public long getTimestampOffsetMillis() {
        return timestampOffsetMillis;
    }

    public double getDistanceMeters() {
        return distanceMeters;
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
}
