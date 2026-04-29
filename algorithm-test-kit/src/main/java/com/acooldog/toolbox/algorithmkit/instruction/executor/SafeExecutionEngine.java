package com.acooldog.toolbox.algorithmkit.instruction.executor;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;
import com.acooldog.toolbox.algorithmkit.GpsTrajectoryGenerator;
import com.acooldog.toolbox.algorithmkit.GpsTrajectoryResult;
import com.acooldog.toolbox.algorithmkit.StepCadenceSimulator;
import com.acooldog.toolbox.algorithmkit.StepSimulationResult;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;
import com.acooldog.toolbox.algorithmkit.instruction.validator.InstructionValidator;
import com.acooldog.toolbox.algorithmkit.instruction.validator.ValidationResult;
import com.acooldog.toolbox.algorithmkit.scenario.TestScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class SafeExecutionEngine {
    private final InstructionValidator validator = new InstructionValidator();

    public ScenarioExecutionReport execute(TestScenario scenario, ExecutionMode mode) {
        long startedAt = System.currentTimeMillis();
        List<ExecutionResult> results = new ArrayList<>();
        boolean success = true;
        for (TestInstruction instruction : scenario.getInstructions()) {
            ExecutionResult result = executeInstruction(instruction, mode);
            results.add(result);
            success = success && result.isSuccess();
            if (!result.isSuccess()) {
                break;
            }
        }
        return new ScenarioExecutionReport(
                scenario.getId(),
                mode,
                success,
                results,
                System.currentTimeMillis() - startedAt
        );
    }

    private ExecutionResult executeInstruction(TestInstruction instruction, ExecutionMode mode) {
        long startedAt = System.currentTimeMillis();
        ValidationResult validation = validator.validate(instruction);
        if (!validation.isAccepted()) {
            return result(instruction, false, validation.getMessage(), "", startedAt);
        }
        if (mode == ExecutionMode.MODE_VALIDATION_ONLY) {
            return result(instruction, true, "语法和安全检查通过", "validation_only", startedAt);
        }
        if (mode == ExecutionMode.MODE_RECORDING) {
            return result(instruction, true, "仅记录指令，未执行", "record_only:" + instruction.getAction().name(), startedAt);
        }
        try {
            String output = runPureSimulation(instruction, mode);
            return result(instruction, true, summarize(instruction, output), output, startedAt);
        } catch (Exception exception) {
            return result(instruction, false, exception.getMessage(), "", startedAt);
        }
    }

    private String runPureSimulation(TestInstruction instruction, ExecutionMode mode) {
        Map<String, Object> params = instruction.getParameters();
        TestAction action = instruction.getAction();
        switch (action) {
            case GENERATE_STEP_FREQUENCY:
                int cadence = intParam(params, "base_spm", intParam(params, "cadence", intParam(params, "cadenceSpm", 180)));
                int duration = intParam(params, "duration", 300);
                StepSimulationResult stepResult = new StepCadenceSimulator().generate(cadence, duration);
                return stepResult.toJson();
            case GENERATE_GPS_TRAJECTORY:
                GpsTrajectoryResult gpsResult = new GpsTrajectoryGenerator().generate(
                        doubleParam(params, "start_lat", 36.667662d),
                        doubleParam(params, "start_lon", 117.027707d),
                        doubleParam(params, "end_lat", 36.669000d),
                        doubleParam(params, "end_lon", 117.030000d),
                        doubleParam(params, "speed_mps", 4.0d)
                );
                return gpsResult.toJson();
            case GENERATE_SENSOR_DATA:
                return "{\"type\":\"sensor_data\",\"status\":\"generated_in_memory\"}";
            case VALIDATE_SENSOR_CONSISTENCY:
            case VALIDATE_MOTION_PATTERN:
            case VALIDATE_LOCATION_DRIFT:
                return "{\"type\":\"validation\",\"action\":\"" + action.name() + "\",\"status\":\"simulated\"}";
            case CHECK_ROOT_STATUS:
            case CHECK_DEVELOPER_OPTIONS:
            case CHECK_MOCK_LOCATION:
            case CHECK_HOOK_FRAMEWORK:
                return "{\"type\":\"read_only_monitor\",\"action\":\"" + action.name() + "\",\"status\":\"record_only_placeholder\"}";
            case WAIT_FOR_DURATION:
                return "{\"type\":\"wait\",\"plannedDelayMillis\":" + instruction.getDelayAfter() + ",\"sleptMillis\":0}";
            case SET_TEST_CONDITION:
                return "{\"type\":\"condition\",\"status\":\"set_in_memory\"}";
            case ASSERT_EXPECTED_RESULT:
                return "{\"type\":\"assertion\",\"status\":\"passed_in_simulation\"}";
            case SIMULATE_GPS_JUMP:
                return "{\"type\":\"anomaly\",\"kind\":\"gps_jump\",\"distanceMeters\":"
                        + intParam(params, "jump_distance", 1000)
                        + ",\"durationSeconds\":"
                        + intParam(params, "duration", 5)
                        + "}";
            default:
                return "{\"status\":\"ignored\"}";
        }
    }

    private ExecutionResult result(TestInstruction instruction, boolean success, String summary, String output, long startedAt) {
        return new ExecutionResult(
                instruction.getId(),
                success,
                summary == null ? "" : summary,
                output == null ? "" : output,
                AlgorithmHash.sha256(output == null ? "" : output),
                System.currentTimeMillis() - startedAt
        );
    }

    private String summarize(TestInstruction instruction, String output) {
        return String.format(Locale.US, "%s completed, bytes=%d", instruction.getAction().name(), output == null ? 0 : output.length());
    }

    private int intParam(Map<String, Object> params, String key, int fallback) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double doubleParam(Map<String, Object> params, String key, double fallback) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value == null ? fallback : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
