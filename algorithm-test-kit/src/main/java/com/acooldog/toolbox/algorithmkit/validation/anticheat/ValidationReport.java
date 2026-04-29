package com.acooldog.toolbox.algorithmkit.validation.anticheat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationReport {
    private final String scenarioId;
    private final List<ValidationItem> validations;
    private final int riskScore;
    private final List<String> recommendations;

    public ValidationReport(String scenarioId, List<ValidationItem> validations, int riskScore, List<String> recommendations) {
        this.scenarioId = scenarioId;
        this.validations = Collections.unmodifiableList(new ArrayList<>(validations));
        this.riskScore = Math.max(0, Math.min(100, riskScore));
        this.recommendations = Collections.unmodifiableList(new ArrayList<>(recommendations));
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public List<ValidationItem> getValidations() {
        return validations;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public String toText() {
        StringBuilder builder = new StringBuilder();
        builder.append("scenario=").append(scenarioId).append(", riskScore=").append(riskScore).append('\n');
        for (ValidationItem item : validations) {
            builder.append("- ").append(item.getName()).append(": ").append(item.getScore()).append(" | ").append(item.getMessage()).append('\n');
        }
        builder.append("recommendations:\n");
        for (String recommendation : recommendations) {
            builder.append("- ").append(recommendation).append('\n');
        }
        return builder.toString();
    }
}
