package com.acooldog.toolbox.sensortest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilderFactory;

public final class GpxNmeaReplayEngine {
    private static final long DEFAULT_START_TIME_MILLIS = 1700000000000L;

    public NmeaReplayReport generate(String gpxContent, NmeaAnomalyMode anomalyMode) {
        List<GpxReplayPoint> points = parseGpx(gpxContent);
        if (points.size() < 2) {
            throw new IllegalArgumentException("GPX must contain at least two track points");
        }
        List<GpxReplayPoint> enrichedPoints = enrichMotion(points);
        List<String> sentences = new ArrayList<>();
        List<RiskFinding> findings = findingsFor(anomalyMode);
        sentences.add(txtSentence(NmeaReplayReport.WATERMARK));
        for (int index = 0; index < enrichedPoints.size(); index++) {
            FrameState state = frameState(anomalyMode, index, enrichedPoints.size());
            GpxReplayPoint point = adjustedPoint(enrichedPoints, index, anomalyMode);
            sentences.add(rmcSentence(point, state.valid));
            sentences.add(ggaSentence(point, state.quality, state.satellites));
        }
        sentences.add(txtSentence(NmeaReplayReport.WATERMARK));
        return new NmeaReplayReport(anomalyMode, enrichedPoints, sentences, findings);
    }

    private List<GpxReplayPoint> parseGpx(String gpxContent) {
        if (gpxContent == null || gpxContent.trim().isEmpty()) {
            throw new IllegalArgumentException("GPX content is empty");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            setFeatureQuietly(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureQuietly(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureQuietly(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(gpxContent)));
            NodeList nodes = document.getElementsByTagNameNS("*", "trkpt");
            if (nodes.getLength() == 0) {
                nodes = document.getElementsByTagName("trkpt");
            }
            List<GpxReplayPoint> points = new ArrayList<>(nodes.getLength());
            for (int index = 0; index < nodes.getLength(); index++) {
                Element element = (Element) nodes.item(index);
                double latitude = Double.parseDouble(element.getAttribute("lat"));
                double longitude = Double.parseDouble(element.getAttribute("lon"));
                double altitude = parseDoubleOrDefault(childText(element, "ele"), 0d);
                long timestamp = parseTimeOrDefault(childText(element, "time"), DEFAULT_START_TIME_MILLIS + (index * 1000L));
                validateCoordinate(latitude, longitude);
                points.add(new GpxReplayPoint(timestamp, latitude, longitude, altitude, 0d, 0d));
            }
            return points;
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) exception;
            }
            throw new IllegalArgumentException("Unable to parse GPX: " + exception.getMessage(), exception);
        }
    }

    private List<GpxReplayPoint> enrichMotion(List<GpxReplayPoint> source) {
        List<GpxReplayPoint> points = new ArrayList<>(source.size());
        for (int index = 0; index < source.size(); index++) {
            GpxReplayPoint current = source.get(index);
            GpxReplayPoint previous = index == 0 ? current : source.get(index - 1);
            GpxReplayPoint next = index + 1 < source.size() ? source.get(index + 1) : current;
            GpxReplayPoint speedFrom = index == 0 ? current : previous;
            GpxReplayPoint speedTo = index == 0 ? next : current;
            double distance = distanceMeters(speedFrom.getLatitude(), speedFrom.getLongitude(), speedTo.getLatitude(), speedTo.getLongitude());
            double durationSeconds = Math.max(1d, Math.abs(speedTo.getTimestampMillis() - speedFrom.getTimestampMillis()) / 1000d);
            double speed = distance / durationSeconds;
            double bearing = bearingDegrees(current.getLatitude(), current.getLongitude(), next.getLatitude(), next.getLongitude());
            points.add(new GpxReplayPoint(
                    current.getTimestampMillis(),
                    current.getLatitude(),
                    current.getLongitude(),
                    current.getAltitudeMeters(),
                    speed,
                    bearing
            ));
        }
        return points;
    }

    private GpxReplayPoint adjustedPoint(List<GpxReplayPoint> points, int index, NmeaAnomalyMode anomalyMode) {
        GpxReplayPoint point = points.get(index);
        if (anomalyMode != NmeaAnomalyMode.SPEED_JUMP || index != points.size() / 2) {
            return point;
        }
        return new GpxReplayPoint(
                point.getTimestampMillis(),
                point.getLatitude(),
                point.getLongitude(),
                point.getAltitudeMeters(),
                Math.max(15d, point.getSpeedMetersPerSecond() * 4d),
                point.getBearingDegrees()
        );
    }

    private FrameState frameState(NmeaAnomalyMode anomalyMode, int index, int count) {
        if (anomalyMode == NmeaAnomalyMode.SIGNAL_LOSS && index >= count / 3 && index <= count / 2) {
            return new FrameState(false, 0, 0);
        }
        if (anomalyMode == NmeaAnomalyMode.SATELLITE_COUNT_JUMP && index == count / 2) {
            return new FrameState(true, 1, 3);
        }
        if (anomalyMode == NmeaAnomalyMode.SATELLITE_COUNT_JUMP && index == (count / 2) + 1) {
            return new FrameState(true, 1, 14);
        }
        return new FrameState(true, 1, 10);
    }

    private List<RiskFinding> findingsFor(NmeaAnomalyMode anomalyMode) {
        List<RiskFinding> findings = new ArrayList<>();
        if (anomalyMode == NmeaAnomalyMode.NORMAL) {
            findings.add(new RiskFinding(
                    "baseline_replay",
                    "info",
                    "GPX points are converted to a valid RMC/GGA sequence with explicit testing watermark.",
                    "baseline trajectory smoothness and NMEA checksum verification"
            ));
        } else if (anomalyMode == NmeaAnomalyMode.SIGNAL_LOSS) {
            findings.add(new RiskFinding(
                    "gps_signal_loss",
                    "high",
                    "A middle window reports invalid RMC status and zero fix quality.",
                    "location continuity should degrade or flag signal-loss windows"
            ));
        } else if (anomalyMode == NmeaAnomalyMode.SATELLITE_COUNT_JUMP) {
            findings.add(new RiskFinding(
                    "satellite_count_jump",
                    "medium",
                    "Satellite count changes abruptly between adjacent GGA frames.",
                    "satellite metadata temporal consistency check"
            ));
        } else if (anomalyMode == NmeaAnomalyMode.SPEED_JUMP) {
            findings.add(new RiskFinding(
                    "gps_speed_jump",
                    "high",
                    "One RMC frame reports an abrupt high speed while coordinates remain close to the GPX path.",
                    "speed and coordinate delta cross-check"
            ));
        }
        return findings;
    }

    private String rmcSentence(GpxReplayPoint point, boolean valid) {
        Date date = new Date(point.getTimestampMillis());
        String body = String.format(
                Locale.US,
                "GPRMC,%s,%s,%s,%s,%s,%s,%.2f,%.2f,%s,,,A",
                timeFormat().format(date),
                valid ? "A" : "V",
                formatLatitude(point.getLatitude()),
                point.getLatitude() >= 0d ? "N" : "S",
                formatLongitude(point.getLongitude()),
                point.getLongitude() >= 0d ? "E" : "W",
                valid ? point.getSpeedMetersPerSecond() * 1.943844492d : 0d,
                point.getBearingDegrees(),
                dateFormat().format(date)
        );
        return "$" + body + "*" + checksum(body);
    }

    private String ggaSentence(GpxReplayPoint point, int quality, int satellites) {
        Date date = new Date(point.getTimestampMillis());
        String body = String.format(
                Locale.US,
                "GPGGA,%s,%s,%s,%s,%s,%d,%02d,0.9,%.1f,M,0.0,M,,",
                timeFormat().format(date),
                formatLatitude(point.getLatitude()),
                point.getLatitude() >= 0d ? "N" : "S",
                formatLongitude(point.getLongitude()),
                point.getLongitude() >= 0d ? "E" : "W",
                quality,
                satellites,
                point.getAltitudeMeters()
        );
        return "$" + body + "*" + checksum(body);
    }

    private String txtSentence(String text) {
        String body = "GPTXT,01,01,02," + text;
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

    private SimpleDateFormat timeFormat() {
        SimpleDateFormat format = new SimpleDateFormat("HHmmss.SS", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private SimpleDateFormat dateFormat() {
        SimpleDateFormat format = new SimpleDateFormat("ddMMyy", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private String childText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagNameNS("*", tagName);
        if (nodes.getLength() == 0) {
            nodes = element.getElementsByTagName(tagName);
        }
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return "";
        }
        return nodes.item(0).getTextContent() == null ? "" : nodes.item(0).getTextContent().trim();
    }

    private double parseDoubleOrDefault(String value, double defaultValue) {
        try {
            return value == null || value.isEmpty() ? defaultValue : Double.parseDouble(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private long parseTimeOrDefault(String value, long defaultValue) {
        try {
            return value == null || value.isEmpty() ? defaultValue : Instant.parse(value).toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return defaultValue;
        }
    }

    private void validateCoordinate(double latitude, double longitude) {
        if (Double.isNaN(latitude) || latitude < -90d || latitude > 90d) {
            throw new IllegalArgumentException("Invalid GPX latitude");
        }
        if (Double.isNaN(longitude) || longitude < -180d || longitude > 180d) {
            throw new IllegalArgumentException("Invalid GPX longitude");
        }
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double radius = 6371008.8d;
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(deltaPhi / 2d) * Math.sin(deltaPhi / 2d)
                + Math.cos(phi1) * Math.cos(phi2)
                * Math.sin(deltaLambda / 2d) * Math.sin(deltaLambda / 2d);
        double c = 2d * Math.atan2(Math.sqrt(a), Math.sqrt(1d - a));
        return radius * c;
    }

    private double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);
        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2)
                - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);
        double degrees = Math.toDegrees(Math.atan2(y, x));
        return (degrees + 360d) % 360d;
    }

    private void setFeatureQuietly(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // XML parsers vary on Android/JVM; unsupported hardening flags are best-effort.
        }
    }

    private static final class FrameState {
        private final boolean valid;
        private final int quality;
        private final int satellites;

        private FrameState(boolean valid, int quality, int satellites) {
            this.valid = valid;
            this.quality = quality;
            this.satellites = satellites;
        }
    }
}
