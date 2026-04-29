package com.acooldog.toolbox.algorithmkit.instruction.dsl;

import java.util.Map;

public interface TestInstruction {
    String getId();

    TestAction getAction();

    Map<String, Object> getParameters();

    long getDelayAfter();

    SafetyLevel getSafetyLevel();
}
