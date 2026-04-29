package com.acooldog.toolbox.sensortest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class NmeaReplayReport {
    public static final String WATERMARK = "FOR TESTING ONLY";

    private final NmeaAnomalyMode anomalyMode;
    private final List<GpxReplayPoint> points;
    private final List<String> nmeaSentences;
    private final List<RiskFinding> findings;

    NmeaReplayReport(
            NmeaAnomalyMode anomalyMode,
            List<GpxReplayPoint> points,
            List<String> nmeaSentences,
            List<RiskFinding> findings
    ) {
        this.anomalyMode = anomalyMode;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        this.nmeaSentences = Collections.unmodifiableList(new ArrayList<>(nmeaSentences));
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    public NmeaAnomalyMode getAnomalyMode() {
        return anomalyMode;
    }

    public List<GpxReplayPoint> getPoints() {
        return points;
    }

    public List<String> getNmeaSentences() {
        return nmeaSentences;
    }

    public List<RiskFinding> getFindings() {
        return findings;
    }

    public String summary() {
        return String.format(
                Locale.US,
                "%s | anomaly=%s, gpxPoints=%d, nmeaSentences=%d, findings=%d",
                WATERMARK,
                anomalyMode,
                points.size(),
                nmeaSentences.size(),
                findings.size()
        );
    }

    public String toNmeaStream() {
        StringBuilder builder = new StringBuilder();
        for (String sentence : nmeaSentences) {
            builder.append(sentence).append('\n');
        }
        return builder.toString();
    }

    public String toHumanReport() {
        StringBuilder builder = new StringBuilder();
        builder.append(WATERMARK).append('\n');
        builder.append("NMEA replay report\n");
        builder.append("anomalyMode=").append(anomalyMode)
                .append(", gpxPoints=").append(points.size())
                .append(", nmeaSentences=").append(nmeaSentences.size())
                .append('\n');
        for (RiskFinding finding : findings) {
            builder.append("- [")
                    .append(finding.getSeverity())
                    .append("] ")
                    .append(finding.getCategory())
                    .append(": ")
                    .append(finding.getDescription())
                    .append(" | expectedDefense=")
                    .append(finding.getExpectedDefenseSignal())
                    .append('\n');
        }
        return builder.toString();
    }

    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"watermark\":\"").append(WATERMARK).append("\",")
                .append("\"type\":\"nmea_replay_report\",")
                .append("\"anomalyMode\":\"").append(anomalyMode).append("\",")
                .append("\"pointCount\":").append(points.size()).append(',')
                .append("\"sentenceCount\":").append(nmeaSentences.size()).append(',')
                .append("\"findings\":[");
        for (int index = 0; index < findings.size(); index++) {
            RiskFinding finding = findings.get(index);
            if (index > 0) {
                builder.append(',');
            }
            builder.append("{\"category\":\"").append(json(finding.getCategory()))
                    .append("\",\"severity\":\"").append(json(finding.getSeverity()))
                    .append("\",\"description\":\"").append(json(finding.getDescription()))
                    .append("\",\"expectedDefense\":\"").append(json(finding.getExpectedDefenseSignal()))
                    .append("\"}");
        }
        builder.append("],\"sentences\":[");
        for (int index = 0; index < nmeaSentences.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(json(nmeaSentences.get(index))).append('"');
        }
        builder.append("]}");
        return builder.toString();
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
