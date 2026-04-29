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
public final class GpsTrajectoryGenerator {
    public GpsTrajectoryResult generate(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            double speedMetersPerSecond
    ) {
        validate(startLatitude, startLongitude, endLatitude, endLongitude, speedMetersPerSecond);
        double distanceMeters = GeoMath.distanceMeters(startLatitude, startLongitude, endLatitude, endLongitude);
        int durationSeconds = Math.max(1, (int) Math.ceil(distanceMeters / speedMetersPerSecond));
        int pointCount = durationSeconds + 1;
        double bearing = GeoMath.bearingDegrees(startLatitude, startLongitude, endLatitude, endLongitude);
        long startTime = 1700000000000L;
        List<TrajectoryPoint> points = new ArrayList<>(pointCount);
        for (int index = 0; index < pointCount; index++) {
            double ratio = pointCount == 1 ? 1d : index / (double) (pointCount - 1);
            double smoothRatio = smoothStep(ratio);
            double latitude = GeoMath.interpolate(startLatitude, endLatitude, smoothRatio);
            double longitude = GeoMath.interpolate(startLongitude, endLongitude, smoothRatio);
            double altitude = 55d + (1.2d * Math.sin(Math.PI * ratio));
            double speed = index == 0 || index == pointCount - 1
                    ? Math.max(0.1d, speedMetersPerSecond * 0.65d)
                    : speedMetersPerSecond;
            points.add(new TrajectoryPoint(
                    startTime + (index * 1000L),
                    latitude,
                    longitude,
                    altitude,
                    speed,
                    bearing
            ));
        }
        return new GpsTrajectoryResult(
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                speedMetersPerSecond,
                distanceMeters,
                durationSeconds,
                points
        );
    }

    private double smoothStep(double ratio) {
        return ratio * ratio * (3d - (2d * ratio));
    }

    private void validate(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            double speedMetersPerSecond
    ) {
        validateCoordinate(startLatitude, startLongitude, "起点");
        validateCoordinate(endLatitude, endLongitude, "终点");
        if (speedMetersPerSecond <= 0d || speedMetersPerSecond > 12d) {
            throw new IllegalArgumentException("运动速度必须在 0-12 m/s 之间");
        }
        double distanceMeters = GeoMath.distanceMeters(startLatitude, startLongitude, endLatitude, endLongitude);
        if (distanceMeters < 1d || distanceMeters > 50000d) {
            throw new IllegalArgumentException("轨迹距离必须在 1m-50km 之间");
        }
    }

    private void validateCoordinate(double latitude, double longitude, String label) {
        if (Double.isNaN(latitude) || latitude < -90d || latitude > 90d) {
            throw new IllegalArgumentException(label + "纬度无效");
        }
        if (Double.isNaN(longitude) || longitude < -180d || longitude > 180d) {
            throw new IllegalArgumentException(label + "经度无效");
        }
    }
}
