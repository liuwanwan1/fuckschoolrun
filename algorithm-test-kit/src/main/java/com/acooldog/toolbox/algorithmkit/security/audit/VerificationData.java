package com.acooldog.toolbox.algorithmkit.security.audit;

public final class VerificationData {
    private final String previousHash;
    private final String currentHash;

    public VerificationData(String previousHash, String currentHash) {
        this.previousHash = previousHash == null ? "" : previousHash;
        this.currentHash = currentHash == null ? "" : currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }
}
