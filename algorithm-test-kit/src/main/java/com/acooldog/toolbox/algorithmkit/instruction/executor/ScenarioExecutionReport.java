package com.acooldog.toolbox.algorithmkit.instruction.executor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ScenarioExecutionReport {
    private final String scenarioId;
    private final ExecutionMode mode;
    private final boolean success;
    private final List<ExecutionResult> results;
    private final long elapsedMillis;

    public ScenarioExecutionReport(String scenarioId, ExecutionMode mode, boolean success, List<ExecutionResult> results, long elapsedMillis) {
        this.scenarioId = scenarioId;
        this.mode = mode;
        this.success = success;
        this.results = Collections.unmodifiableList(new ArrayList<>(results));
        this.elapsedMillis = elapsedMillis;
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public ExecutionMode getMode() {
        return mode;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<ExecutionResult> getResults() {
        return results;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }

    public String toTextReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("scenario=").append(scenarioId)
                .append(", mode=").append(mode.name())
                .append(", success=").append(success)
                .append(", elapsed=").append(elapsedMillis).append("ms\n");
        for (ExecutionResult result : results) {
            builder.append("- ")
                    .append(result.getInstructionId())
                    .append(" | ")
                    .append(result.isSuccess() ? "OK" : "FAIL")
                    .append(" | ")
                    .append(result.getSummary())
                    .append(" | hash=")
                    .append(result.getOutputHash())
                    .append('\n');
        }
        return builder.toString();
    }
}
