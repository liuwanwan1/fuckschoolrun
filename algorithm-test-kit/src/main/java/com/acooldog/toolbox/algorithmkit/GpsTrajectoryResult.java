package com.acooldog.toolbox.algorithmkit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class GpsTrajectoryResult {
    private final double startLatitude;
    private final double startLongitude;
    private final double endLatitude;
    private final double endLongitude;
    private final double speedMetersPerSecond;
    private final double distanceMeters;
    private final int durationSeconds;
    private final List<TrajectoryPoint> points;

    GpsTrajectoryResult(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude,
            double speedMetersPerSecond,
            double distanceMeters,
            int durationSeconds,
            List<TrajectoryPoint> points
    ) {
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    public double getStartLatitude() {
        return startLatitude;
    }

    public double getStartLongitude() {
        return startLongitude;
    }

    public double getEndLatitude() {
        return endLatitude;
    }

    public double getEndLongitude() {
        return endLongitude;
    }

    public double getSpeedMetersPerSecond() {
        return speedMetersPerSecond;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public List<TrajectoryPoint> getPoints() {
        return points;
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(
                Locale.US,
                "{\"type\":\"gps_trajectory\",\"start\":{\"lat\":%.8f,\"lon\":%.8f},\"end\":{\"lat\":%.8f,\"lon\":%.8f},\"speedMps\":%.3f,\"distanceMeters\":%.3f,\"durationSeconds\":%d,\"points\":[",
                startLatitude,
                startLongitude,
                endLatitude,
                endLongitude,
                speedMetersPerSecond,
                distanceMeters,
                durationSeconds
        ));
        for (int index = 0; index < points.size(); index++) {
            TrajectoryPoint point = points.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(
                    Locale.US,
                    "{\"t\":%d,\"lat\":%.8f,\"lon\":%.8f,\"alt\":%.2f,\"speed\":%.3f,\"bearing\":%.2f}",
                    point.getTimestampMillis(),
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getAltitudeMeters(),
                    point.getSpeedMetersPerSecond(),
                    point.getBearingDegrees()
            ));
        }
        builder.append("]}");
        return builder.toString();
    }

    public String toNmeaGprmc() {
        StringBuilder builder = new StringBuilder();
        for (TrajectoryPoint point : points) {
            builder.append(toRmcSentence(point)).append('\n');
        }
        return builder.toString();
    }

    public String toGpx() {
        SimpleDateFormat format = isoUtcFormat();
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<gpx version=\"1.1\" creator=\"algorithm-test-kit\">\n")
                .append("  <trk><name>algorithm-test-trajectory</name><trkseg>\n");
        for (TrajectoryPoint point : points) {
            builder.append(String.format(
                    Locale.US,
                    "    <trkpt lat=\"%.8f\" lon=\"%.8f\"><ele>%.2f</ele><time>%s</time></trkpt>\n",
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getAltitudeMeters(),
                    format.format(new Date(point.getTimestampMillis()))
            ));
        }
        builder.append("  </trkseg></trk>\n</gpx>\n");
        return builder.toString();
    }

    public String toKml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n")
                .append("  <Document><Placemark><name>algorithm-test-trajectory</name><LineString><coordinates>\n");
        for (TrajectoryPoint point : points) {
            builder.append(String.format(
                    Locale.US,
                    "    %.8f,%.8f,%.2f\n",
                    point.getLongitude(),
                    point.getLatitude(),
                    point.getAltitudeMeters()
            ));
        }
        builder.append("  </coordinates></LineString></Placemark></Document>\n</kml>\n");
        return builder.toString();
    }

    public String toCsv() {
        StringBuilder builder = new StringBuilder("timestamp_ms,latitude,longitude,altitude_m,speed_mps,bearing_deg\n");
        for (TrajectoryPoint point : points) {
            builder.append(String.format(
                    Locale.US,
                    "%d,%.8f,%.8f,%.2f,%.3f,%.2f\n",
                    point.getTimestampMillis(),
                    point.getLatitude(),
                    point.getLongitude(),
                    point.getAltitudeMeters(),
                    point.getSpeedMetersPerSecond(),
                    point.getBearingDegrees()
            ));
        }
        return builder.toString();
    }

    public String outputSummary() {
        return String.format(Locale.US, "轨迹点=%d, 距离=%.1fm", points.size(), distanceMeters);
    }

    private String toRmcSentence(TrajectoryPoint point) {
        Date date = new Date(point.getTimestampMillis());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss.SS", Locale.US);
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy", Locale.US);
        timeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String body = String.format(
                Locale.US,
                "GPRMC,%s,A,%s,%s,%s,%s,%.2f,%.2f,%s,,,A",
                timeFormat.format(date),
                formatLatitude(point.getLatitude()),
                point.getLatitude() >= 0d ? "N" : "S",
                formatLongitude(point.getLongitude()),
                point.getLongitude() >= 0d ? "E" : "W",
                point.getSpeedMetersPerSecond() * 1.943844492d,
                point.getBearingDegrees(),
                dateFormat.format(date)
        );
        return "$" + body + "*" + checksum(body);
    }

    private String formatLatitude(double latitude) {
        double abs = Math.abs(latitude);
        int degrees = (int) abs;
        double minutes = (abs - degrees) * 60d;
        return String.format(Locale.US, "%02d%07.4f", degrees, minutes);
    }

    private String formatLongitude(double longitude) {
        double abs = Math.abs(longitude);
        int degrees = (int) abs;
        double minutes = (abs - degrees) * 60d;
        return String.format(Locale.US, "%03d%07.4f", degrees, minutes);
    }

    private String checksum(String body) {
        int checksum = 0;
        for (int index = 0; index < body.length(); index++) {
            checksum ^= body.charAt(index);
        }
        return String.format(Locale.US, "%02X", checksum);
    }

    private SimpleDateFormat isoUtcFormat() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }
}
