package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

public final class RootShellProbeResult {
    private final boolean authorized;
    private final boolean timedOut;
    private final int exitCode;
    private final String output;

    public RootShellProbeResult(boolean authorized, boolean timedOut, int exitCode, @NonNull String output) {
        this.authorized = authorized;
        this.timedOut = timedOut;
        this.exitCode = exitCode;
        this.output = output;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public int getExitCode() {
        return exitCode;
    }

    @NonNull
    public String getOutput() {
        return output;
    }
}
