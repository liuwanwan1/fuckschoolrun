package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class RootGmTestData {
    private final String routeId;
    private final double requestedSpeedMetersPerSecond;
    private final double requestedDistanceMeters;
    private final double generatedDistanceMeters;
    private final List<RootGmSamplePoint> points;

    RootGmTestData(
            @NonNull String routeId,
            double requestedSpeedMetersPerSecond,
            double requestedDistanceMeters,
            double generatedDistanceMeters,
            @NonNull List<RootGmSamplePoint> points
    ) {
        this.routeId = routeId;
        this.requestedSpeedMetersPerSecond = requestedSpeedMetersPerSecond;
        this.requestedDistanceMeters = requestedDistanceMeters;
        this.generatedDistanceMeters = generatedDistanceMeters;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    @NonNull
    public String getRouteId() {
        return routeId;
    }

    public double getRequestedSpeedMetersPerSecond() {
        return requestedSpeedMetersPerSecond;
    }

    public double getRequestedDistanceMeters() {
        return requestedDistanceMeters;
    }

    public double getGeneratedDistanceMeters() {
        return generatedDistanceMeters;
    }

    @NonNull
    public List<RootGmSamplePoint> getPoints() {
        return points;
    }

    @NonNull
    public String summary() {
        return String.format(
                Locale.getDefault(),
                "FOR TESTING ONLY GM数据：route=%s, points=%d, speed=%.2fm/s, distance=%.1fm",
                routeId,
                points.size(),
                requestedSpeedMetersPerSecond,
                generatedDistanceMeters
        );
    }

    @NonNull
    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(
                Locale.US,
                "{\"watermark\":\"FOR TESTING ONLY\",\"routeId\":\"%s\",\"speedMps\":%.3f,\"requestedDistanceMeters\":%.3f,\"generatedDistanceMeters\":%.3f,\"points\":[",
                escape(routeId),
                requestedSpeedMetersPerSecond,
                requestedDistanceMeters,
                generatedDistanceMeters
        ));
        for (int index = 0; index < points.size(); index++) {
            RootGmSamplePoint point = points.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(
                    Locale.US,
                    "{\"t\":%d,\"distance\":%.3f,\"lat\":%.8f,\"lon\":%.8f,\"alt\":%.2f,\"speed\":%.3f}",
                    point.getTimestampOffsetMillis(),
                    point.getDistanceMeters(),
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getAltitudeMeters(),
                    point.getSpeedMetersPerSecond()
            ));
        }
        builder.append("]}");
        return builder.toString();
    }

    @NonNull
    private String escape(@NonNull String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
