package com.acooldog.toolbox.sensortest;

public final class EnvironmentSurfaceSnapshot {
    private final boolean rootManagerDetected;
    private final boolean suPathDetected;
    private final boolean hookFrameworkDetected;
    private final boolean developerOptionsEnabled;
    private final boolean mockLocationAllowed;
    private final String simulatedCellLac;
    private final String simulatedCellCid;
    private final String simulatedWifiBssid;
    private final String declaredRegion;

    public EnvironmentSurfaceSnapshot(
            boolean rootManagerDetected,
            boolean suPathDetected,
            boolean hookFrameworkDetected,
            boolean developerOptionsEnabled,
            boolean mockLocationAllowed,
            String simulatedCellLac,
            String simulatedCellCid,
            String simulatedWifiBssid,
            String declaredRegion
    ) {
        this.rootManagerDetected = rootManagerDetected;
        this.suPathDetected = suPathDetected;
        this.hookFrameworkDetected = hookFrameworkDetected;
        this.developerOptionsEnabled = developerOptionsEnabled;
        this.mockLocationAllowed = mockLocationAllowed;
        this.simulatedCellLac = emptyToUnknown(simulatedCellLac);
        this.simulatedCellCid = emptyToUnknown(simulatedCellCid);
        this.simulatedWifiBssid = emptyToUnknown(simulatedWifiBssid);
        this.declaredRegion = emptyToUnknown(declaredRegion);
    }

    public boolean isRootManagerDetected() {
        return rootManagerDetected;
    }

    public boolean isSuPathDetected() {
        return suPathDetected;
    }

    public boolean isHookFrameworkDetected() {
        return hookFrameworkDetected;
    }

    public boolean isDeveloperOptionsEnabled() {
        return developerOptionsEnabled;
    }

    public boolean isMockLocationAllowed() {
        return mockLocationAllowed;
    }

    public String getSimulatedCellLac() {
        return simulatedCellLac;
    }

    public String getSimulatedCellCid() {
        return simulatedCellCid;
    }

    public String getSimulatedWifiBssid() {
        return simulatedWifiBssid;
    }

    public String getDeclaredRegion() {
        return declaredRegion;
    }

    private static String emptyToUnknown(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }
}
