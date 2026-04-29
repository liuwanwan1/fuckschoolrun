package com.acooldog.toolbox.algorithmkit.instruction.dsl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BasicTestInstruction implements TestInstruction {
    private final String id;
    private final TestAction action;
    private final Map<String, Object> parameters;
    private final long delayAfter;
    private final SafetyLevel safetyLevel;

    public BasicTestInstruction(
            String id,
            TestAction action,
            Map<String, Object> parameters,
            long delayAfter,
            SafetyLevel safetyLevel
    ) {
        this.id = normalize(id, "instruction_" + System.currentTimeMillis());
        this.action = action == null ? TestAction.WAIT_FOR_DURATION : action;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters == null ? Collections.emptyMap() : parameters));
        this.delayAfter = Math.max(0L, delayAfter);
        this.safetyLevel = safetyLevel == null ? SafetyLevel.SAFE_SIMULATION : safetyLevel;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TestAction getAction() {
        return action;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public long getDelayAfter() {
        return delayAfter;
    }

    @Override
    public SafetyLevel getSafetyLevel() {
        return safetyLevel;
    }

    public BasicTestInstruction withDelay(long delayMillis) {
        return new BasicTestInstruction(id, action, parameters, delayMillis, safetyLevel);
    }

    public BasicTestInstruction withParameter(String key, Object value) {
        Map<String, Object> next = new LinkedHashMap<>(parameters);
        next.put(key, value);
        return new BasicTestInstruction(id, action, next, delayAfter, safetyLevel);
    }

    private String normalize(String raw, String fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return raw.trim();
    }
}
