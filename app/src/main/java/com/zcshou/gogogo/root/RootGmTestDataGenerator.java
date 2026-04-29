package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.route.domain.model.RouteDefinition;
import com.acooldog.toolbox.route.domain.model.RoutePoint;

import java.util.ArrayList;
import java.util.List;

public final class RootGmTestDataGenerator {
    private static final double EARTH_RADIUS_METERS = 6371000d;
    private static final int MAX_SAMPLE_COUNT = 180;

    @NonNull
    public RootGmTestData generate(
            @NonNull RouteDefinition routeDefinition,
            double speedMetersPerSecond,
            double requestedDistanceMeters,
            long intervalMillis
    ) {
        if (!routeDefinition.hasEnoughPoints()) {
            throw new IllegalArgumentException("Route must contain at least two points");
        }
        if (speedMetersPerSecond <= 0d || speedMetersPerSecond > 12d) {
            throw new IllegalArgumentException("speedMetersPerSecond must be in (0, 12]");
        }
        if (requestedDistanceMeters <= 0d) {
            throw new IllegalArgumentException("requestedDistanceMeters must be positive");
        }
        if (intervalMillis <= 0L) {
            throw new IllegalArgumentException("intervalMillis must be positive");
        }
        List<RoutePoint> routePoints = routeDefinition.getPoints();
        double routeDistanceMeters = routeDistanceMeters(routePoints);
        double targetDistanceMeters = Math.min(routeDistanceMeters, requestedDistanceMeters);
        double sampleStepMeters = Math.max(0.5d, speedMetersPerSecond * (intervalMillis / 1000.0d));
        int sampleCount = Math.min(
                MAX_SAMPLE_COUNT,
                Math.max(2, (int) Math.ceil(targetDistanceMeters / sampleStepMeters) + 1)
        );
        double distanceStepMeters = sampleCount <= 1 ? 0d : targetDistanceMeters / (sampleCount - 1);
        List<RootGmSamplePoint> samples = new ArrayList<>(sampleCount);
        for (int index = 0; index < sampleCount; index++) {
            double distanceMeters = Math.min(targetDistanceMeters, index * distanceStepMeters);
            RoutePoint point = pointAtDistance(routePoints, distanceMeters);
            samples.add(new RootGmSamplePoint(
                    index * intervalMillis,
                    distanceMeters,
                    point.getWgsLatitude(),
                    point.getWgsLongitude(),
                    point.getAltitude(),
                    speedMetersPerSecond
            ));
        }
        return new RootGmTestData(
                routeDefinition.getId(),
                speedMetersPerSecond,
                requestedDistanceMeters,
                targetDistanceMeters,
                samples
        );
    }

    private double routeDistanceMeters(@NonNull List<RoutePoint> points) {
        double distance = 0d;
        for (int index = 0; index < points.size() - 1; index++) {
            distance += distanceMeters(points.get(index), points.get(index + 1));
        }
        return distance;
    }

    @NonNull
    private RoutePoint pointAtDistance(@NonNull List<RoutePoint> points, double targetDistanceMeters) {
        double remaining = Math.max(0d, targetDistanceMeters);
        for (int index = 0; index < points.size() - 1; index++) {
            RoutePoint start = points.get(index);
            RoutePoint end = points.get(index + 1);
            double segmentDistance = distanceMeters(start, end);
            if (segmentDistance <= 0.000001d) {
                continue;
            }
            if (remaining <= segmentDistance || index == points.size() - 2) {
                double ratio = Math.max(0d, Math.min(1d, remaining / segmentDistance));
                return interpolate(start, end, ratio);
            }
            remaining -= segmentDistance;
        }
        return points.get(points.size() - 1);
    }

    @NonNull
    private RoutePoint interpolate(@NonNull RoutePoint start, @NonNull RoutePoint end, double ratio) {
        return new RoutePoint(
                lerp(start.getBdLongitude(), end.getBdLongitude(), ratio),
                lerp(start.getBdLatitude(), end.getBdLatitude(), ratio),
                lerp(start.getWgsLongitude(), end.getWgsLongitude(), ratio),
                lerp(start.getWgsLatitude(), end.getWgsLatitude(), ratio),
                lerp(start.getAltitude(), end.getAltitude(), ratio)
        );
    }

    private double lerp(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    private double distanceMeters(@NonNull RoutePoint start, @NonNull RoutePoint end) {
        double lat1 = Math.toRadians(start.getWgsLatitude());
        double lat2 = Math.toRadians(end.getWgsLatitude());
        double deltaLat = lat2 - lat1;
        double deltaLon = Math.toRadians(end.getWgsLongitude() - start.getWgsLongitude());
        double a = Math.sin(deltaLat / 2d) * Math.sin(deltaLat / 2d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2d) * Math.sin(deltaLon / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return EARTH_RADIUS_METERS * c;
    }
}
