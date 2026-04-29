package com.acooldog.toolbox.sensortest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EnvironmentSurfaceAnalyzer {
    public EnvironmentSurfaceReport analyze(EnvironmentSurfaceSnapshot snapshot) {
        List<RiskFinding> findings = new ArrayList<>();
        if (snapshot.isRootManagerDetected() && !snapshot.isSuPathDetected()) {
            findings.add(new RiskFinding(
                    "root_visibility_gap",
                    "high",
                    "Root manager is visible while common su paths are absent; this is a useful blind-spot test for hidden-root detection.",
                    "compare package, binary, mount, property, and shell authorization signals"
            ));
        }
        if (snapshot.isHookFrameworkDetected()) {
            findings.add(new RiskFinding(
                    "hook_framework_presence",
                    "medium",
                    "Hook framework packages are visible and should be treated as an elevated test environment indicator.",
                    "flag instrumentation or hook-framework presence before trusting sensor/location streams"
            ));
        }
        if (snapshot.isDeveloperOptionsEnabled()) {
            findings.add(new RiskFinding(
                    "developer_options",
                    "low",
                    "Developer options are enabled; apps should record this as environment context, not as a standalone fraud signal.",
                    "combine developer-options state with stronger evidence"
            ));
        }
        if (snapshot.isMockLocationAllowed()) {
            findings.add(new RiskFinding(
                    "mock_location_permission",
                    "medium",
                    "Current app has mock-location authorization in the environment snapshot.",
                    "cross-check AppOps mock-location state with provider and GNSS metadata"
            ));
        }
        if (looksLikeCellScenario(snapshot)) {
            findings.add(new RiskFinding(
                    "cellular_signal_scenario",
                    "medium",
                    String.format(
                            Locale.US,
                            "Cell test vector LAC/CID=%s/%s is present for region %s.",
                            snapshot.getSimulatedCellLac(),
                            snapshot.getSimulatedCellCid(),
                            snapshot.getDeclaredRegion()
                    ),
                    "validate cell identity freshness and consistency with GNSS/WiFi region"
            ));
        }
        if (looksLikeWifiScenario(snapshot)) {
            findings.add(new RiskFinding(
                    "wifi_bssid_scenario",
                    "medium",
                    "WiFi BSSID test vector is present; location logic should not trust BSSID alone.",
                    "compare BSSID, scan age, SSID entropy, GNSS, cell, and network-provider signals"
            ));
        }
        if (findings.isEmpty()) {
            findings.add(new RiskFinding(
                    "baseline_environment",
                    "info",
                    "No strong environment attack-surface indicators were reported in this snapshot.",
                    "keep baseline telemetry for regression comparison"
            ));
        }
        return new EnvironmentSurfaceReport(snapshot, findings);
    }

    private boolean looksLikeCellScenario(EnvironmentSurfaceSnapshot snapshot) {
        return !"unknown".equals(snapshot.getSimulatedCellLac()) || !"unknown".equals(snapshot.getSimulatedCellCid());
    }

    private boolean looksLikeWifiScenario(EnvironmentSurfaceSnapshot snapshot) {
        String bssid = snapshot.getSimulatedWifiBssid();
        return !"unknown".equals(bssid) && bssid.split(":").length == 6;
    }
}
