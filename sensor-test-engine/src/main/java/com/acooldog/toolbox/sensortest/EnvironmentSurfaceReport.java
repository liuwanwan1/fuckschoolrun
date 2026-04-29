package com.acooldog.toolbox.sensortest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EnvironmentSurfaceReport {
    public static final String WATERMARK = "FOR TESTING ONLY";

    private final EnvironmentSurfaceSnapshot snapshot;
    private final List<RiskFinding> findings;

    EnvironmentSurfaceReport(EnvironmentSurfaceSnapshot snapshot, List<RiskFinding> findings) {
        this.snapshot = snapshot;
        this.findings = Collections.unmodifiableList(new ArrayList<>(findings));
    }

    public EnvironmentSurfaceSnapshot getSnapshot() {
        return snapshot;
    }

    public List<RiskFinding> getFindings() {
        return findings;
    }

    public String summary() {
        return WATERMARK + " | environment findings=" + findings.size()
                + ", region=" + snapshot.getDeclaredRegion()
                + ", cell=" + snapshot.getSimulatedCellLac() + "/" + snapshot.getSimulatedCellCid()
                + ", wifi=" + snapshot.getSimulatedWifiBssid();
    }

    public String toHumanReport() {
        StringBuilder builder = new StringBuilder();
        builder.append(WATERMARK).append('\n');
        builder.append("Environment attack-surface report\n");
        builder.append("rootManagerDetected=").append(snapshot.isRootManagerDetected())
                .append(", suPathDetected=").append(snapshot.isSuPathDetected())
                .append(", hookFrameworkDetected=").append(snapshot.isHookFrameworkDetected())
                .append(", developerOptionsEnabled=").append(snapshot.isDeveloperOptionsEnabled())
                .append(", mockLocationAllowed=").append(snapshot.isMockLocationAllowed())
                .append('\n');
        builder.append("declaredRegion=").append(snapshot.getDeclaredRegion())
                .append(", simulatedCell=").append(snapshot.getSimulatedCellLac())
                .append('/').append(snapshot.getSimulatedCellCid())
                .append(", simulatedWifi=").append(snapshot.getSimulatedWifiBssid())
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
                .append("\"type\":\"environment_surface_report\",")
                .append("\"rootManagerDetected\":").append(snapshot.isRootManagerDetected()).append(',')
                .append("\"suPathDetected\":").append(snapshot.isSuPathDetected()).append(',')
                .append("\"hookFrameworkDetected\":").append(snapshot.isHookFrameworkDetected()).append(',')
                .append("\"developerOptionsEnabled\":").append(snapshot.isDeveloperOptionsEnabled()).append(',')
                .append("\"mockLocationAllowed\":").append(snapshot.isMockLocationAllowed()).append(',')
                .append("\"declaredRegion\":\"").append(json(snapshot.getDeclaredRegion())).append("\",")
                .append("\"simulatedCellLac\":\"").append(json(snapshot.getSimulatedCellLac())).append("\",")
                .append("\"simulatedCellCid\":\"").append(json(snapshot.getSimulatedCellCid())).append("\",")
                .append("\"simulatedWifiBssid\":\"").append(json(snapshot.getSimulatedWifiBssid())).append("\",")
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
