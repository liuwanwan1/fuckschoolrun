package com.acooldog.toolbox.sensortest;

public final class RiskFinding {
    private final String category;
    private final String severity;
    private final String description;
    private final String expectedDefenseSignal;

    public RiskFinding(String category, String severity, String description, String expectedDefenseSignal) {
        this.category = category;
        this.severity = severity;
        this.description = description;
        this.expectedDefenseSignal = expectedDefenseSignal;
    }

    public String getCategory() {
        return category;
    }

    public String getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public String getExpectedDefenseSignal() {
        return expectedDefenseSignal;
    }
}
