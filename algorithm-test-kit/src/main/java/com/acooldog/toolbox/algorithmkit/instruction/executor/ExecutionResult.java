package com.acooldog.toolbox.algorithmkit.instruction.executor;

public final class ExecutionResult {
    private final String instructionId;
    private final boolean success;
    private final String summary;
    private final String output;
    private final String outputHash;
    private final long elapsedMillis;

    public ExecutionResult(String instructionId, boolean success, String summary, String output, String outputHash, long elapsedMillis) {
        this.instructionId = instructionId;
        this.success = success;
        this.summary = summary;
        this.output = output;
        this.outputHash = outputHash;
        this.elapsedMillis = elapsedMillis;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSummary() {
        return summary;
    }

    public String getOutput() {
        return output;
    }

    public String getOutputHash() {
        return outputHash;
    }

    public long getElapsedMillis() {
        return elapsedMillis;
    }
}
