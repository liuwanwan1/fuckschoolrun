package com.acooldog.toolbox.algorithmkit;

final class GeoMath {
    static final double EARTH_RADIUS_METERS = 6371008.8d;

    private GeoMath() {
    }

    static double distanceMeters(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        double lat1 = Math.toRadians(startLatitude);
        double lat2 = Math.toRadians(endLatitude);
        double dLat = Math.toRadians(endLatitude - startLatitude);
        double dLon = Math.toRadians(endLongitude - startLongitude);
        double a = Math.sin(dLat / 2d) * Math.sin(dLat / 2d)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2d) * Math.sin(dLon / 2d);
        return 2d * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
    }

    static double bearingDegrees(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        double lat1 = Math.toRadians(startLatitude);
        double lat2 = Math.toRadians(endLatitude);
        double dLon = Math.toRadians(endLongitude - startLongitude);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360d) % 360d;
    }

    static double interpolate(double start, double end, double ratio) {
        return start + ((end - start) * ratio);
    }

    static String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
