package com.acooldog.toolbox.algorithmkit.security.audit;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;
import com.acooldog.toolbox.algorithmkit.scenario.ScenarioSerializer;
import com.acooldog.toolbox.algorithmkit.scenario.TestScenario;

import java.util.Collections;

public final class VerifiableLog {
    private final String logId;
    private final long timestamp;
    private final String operator;
    private final TestInstruction instruction;
    private final String inputHash;
    private final String outputHash;
    private final String signature;
    private final VerificationData verificationData;

    public VerifiableLog(
            String logId,
            long timestamp,
            String operator,
            TestInstruction instruction,
            String inputHash,
            String outputHash,
            String signature,
            VerificationData verificationData
    ) {
        this.logId = logId;
        this.timestamp = timestamp;
        this.operator = operator == null ? "" : operator;
        this.instruction = instruction;
        this.inputHash = inputHash == null ? "" : inputHash;
        this.outputHash = outputHash == null ? "" : outputHash;
        this.signature = signature == null ? "" : signature;
        this.verificationData = verificationData;
    }

    public VerificationResult verify() {
        String expectedSignature = sign(logId, timestamp, operator, instruction, inputHash, outputHash, verificationData);
        if (!expectedSignature.equals(signature)) {
            return VerificationResult.invalid("签名验证失败");
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > 30L * 24L * 60L * 60L * 1000L) {
            return VerificationResult.invalid("时间戳异常");
        }
        if (verificationData == null || verificationData.getCurrentHash().trim().isEmpty()) {
            return VerificationResult.invalid("哈希链断裂");
        }
        return VerificationResult.valid("日志验证通过");
    }

    public static String sign(
            String logId,
            long timestamp,
            String operator,
            TestInstruction instruction,
            String inputHash,
            String outputHash,
            VerificationData verificationData
    ) {
        TestScenario scenario = new TestScenario("log", "log", "", "audit", Collections.singletonList(instruction));
        String payload = logId + "|" + timestamp + "|" + operator + "|"
                + ScenarioSerializer.toJson(scenario) + "|" + inputHash + "|" + outputHash + "|"
                + (verificationData == null ? "" : verificationData.getPreviousHash()) + "|"
                + (verificationData == null ? "" : verificationData.getCurrentHash());
        return AlgorithmHash.sha256(payload);
    }

    public String getLogId() {
        return logId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getOperator() {
        return operator;
    }

    public String getInputHash() {
        return inputHash;
    }

    public String getOutputHash() {
        return outputHash;
    }

    public String getSignature() {
        return signature;
    }

    public VerificationData getVerificationData() {
        return verificationData;
    }
}
