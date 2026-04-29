package com.acooldog.toolbox.algorithmkit.security.audit;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

public final class EnhancedAuditLogger {
    private String previousHash = "";

    public VerifiableLog createLog(String operator, TestInstruction instruction, String input, String output) {
        String logId = "log_" + System.nanoTime();
        long timestamp = System.currentTimeMillis();
        String inputHash = AlgorithmHash.sha256(input == null ? "" : input);
        String outputHash = AlgorithmHash.sha256(output == null ? "" : output);
        String currentHash = AlgorithmHash.sha256(previousHash + "|" + logId + "|" + inputHash + "|" + outputHash);
        VerificationData verificationData = new VerificationData(previousHash, currentHash);
        String signature = VerifiableLog.sign(logId, timestamp, operator, instruction, inputHash, outputHash, verificationData);
        previousHash = currentHash;
        return new VerifiableLog(logId, timestamp, operator, instruction, inputHash, outputHash, signature, verificationData);
    }
}
