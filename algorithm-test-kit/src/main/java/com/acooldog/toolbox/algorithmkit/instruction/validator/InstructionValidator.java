package com.acooldog.toolbox.algorithmkit.instruction.validator;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.SafetyLevel;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

public final class InstructionValidator {
    public static final long MAX_ALLOWED_DURATION_MILLIS = 5L * 60L * 1000L;
    public static final long SANDBOX_MEMORY_LIMIT_BYTES = 256L * 1024L * 1024L;

    public ValidationResult validate(TestInstruction instruction) {
        if (instruction == null) {
            return ValidationResult.rejected("指令不能为空");
        }
        if (instruction.getId() == null || instruction.getId().trim().isEmpty()) {
            return ValidationResult.rejected("指令ID不能为空");
        }
        if (instruction.getAction() == null) {
            return ValidationResult.rejected("指令动作不能为空");
        }
        if (modifiesSystemState(instruction)) {
            return ValidationResult.rejected("禁止修改系统状态");
        }
        long duration = calculateSafeDuration(instruction);
        if (duration > MAX_ALLOWED_DURATION_MILLIS) {
            return ValidationResult.rejected("执行时间超过安全限制");
        }
        if (!isSafetyLevelAllowed(instruction)) {
            return ValidationResult.rejected("指令安全级别与动作不匹配");
        }
        if (!hasSufficientMemory(instruction)) {
            return ValidationResult.rejected("内存需求超出限制");
        }
        if (!isValidSyntax(instruction)) {
            return ValidationResult.rejected("指令语法错误");
        }
        return ValidationResult.accepted(duration, calculateMemoryLimit(instruction));
    }

    public boolean modifiesSystemState(TestInstruction instruction) {
        return false;
    }

    public boolean requiresRootAccess(TestInstruction instruction) {
        return false;
    }

    public long calculateSafeDuration(TestInstruction instruction) {
        if (instruction.getAction() == TestAction.WAIT_FOR_DURATION) {
            return Math.min(MAX_ALLOWED_DURATION_MILLIS, Math.max(0L, instruction.getDelayAfter()));
        }
        Object duration = instruction.getParameters().get("duration");
        if (duration instanceof Number) {
            return Math.min(MAX_ALLOWED_DURATION_MILLIS, ((Number) duration).longValue() * 1000L);
        }
        return Math.min(MAX_ALLOWED_DURATION_MILLIS, Math.max(1000L, instruction.getDelayAfter()));
    }

    public long calculateMemoryLimit(TestInstruction instruction) {
        if (instruction.getAction() == TestAction.GENERATE_GPS_TRAJECTORY
                || instruction.getAction() == TestAction.GENERATE_STEP_FREQUENCY) {
            return 96L * 1024L * 1024L;
        }
        return 32L * 1024L * 1024L;
    }

    private boolean isSafetyLevelAllowed(TestInstruction instruction) {
        TestAction action = instruction.getAction();
        SafetyLevel safetyLevel = instruction.getSafetyLevel();
        if (action == TestAction.CHECK_ROOT_STATUS
                || action == TestAction.CHECK_DEVELOPER_OPTIONS
                || action == TestAction.CHECK_MOCK_LOCATION
                || action == TestAction.CHECK_HOOK_FRAMEWORK) {
            return safetyLevel == SafetyLevel.READ_ONLY_MONITOR || safetyLevel == SafetyLevel.RECORD_ONLY;
        }
        return safetyLevel == SafetyLevel.SAFE_SIMULATION || safetyLevel == SafetyLevel.SANDBOX_EXECUTION;
    }

    private boolean hasSufficientMemory(TestInstruction instruction) {
        return calculateMemoryLimit(instruction) <= SANDBOX_MEMORY_LIMIT_BYTES;
    }

    private boolean isValidSyntax(TestInstruction instruction) {
        switch (instruction.getAction()) {
            case GENERATE_STEP_FREQUENCY:
                return hasAny(instruction, "base_spm", "cadence", "cadenceSpm");
            case GENERATE_GPS_TRAJECTORY:
                return !instruction.getParameters().isEmpty();
            case WAIT_FOR_DURATION:
                return instruction.getDelayAfter() <= MAX_ALLOWED_DURATION_MILLIS;
            default:
                return true;
        }
    }

    private boolean hasAny(TestInstruction instruction, String... keys) {
        for (String key : keys) {
            if (instruction.getParameters().containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
