package com.acooldog.toolbox.algorithmkit.scenario;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.BasicTestInstruction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.SafetyLevel;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioTemplateLibrary {
    public List<TestScenario> defaults() {
        List<TestScenario> scenarios = new ArrayList<>();
        scenarios.add(morningRunNormal());
        scenarios.add(anomalyGpsJump());
        return scenarios;
    }

    private TestScenario morningRunNormal() {
        List<TestInstruction> instructions = new ArrayList<>();
        Map<String, Object> gpsParams = new LinkedHashMap<>();
        gpsParams.put("type", "running_track");
        gpsParams.put("speed", "6:00-6:30");
        instructions.add(new BasicTestInstruction(
                "gps_track",
                TestAction.GENERATE_GPS_TRAJECTORY,
                gpsParams,
                0L,
                SafetyLevel.SAFE_SIMULATION
        ));
        Map<String, Object> stepParams = new LinkedHashMap<>();
        stepParams.put("base_spm", 180);
        stepParams.put("variation", 5);
        stepParams.put("interval_acceleration", true);
        instructions.add(new BasicTestInstruction(
                "step_frequency",
                TestAction.GENERATE_STEP_FREQUENCY,
                stepParams,
                0L,
                SafetyLevel.SAFE_SIMULATION
        ));
        return new TestScenario("morning_run_normal", "晨跑-正常模式", "模拟早晨操场跑步，匀速+间歇加速", "normal", instructions);
    }

    private TestScenario anomalyGpsJump() {
        List<TestInstruction> instructions = new ArrayList<>();
        Map<String, Object> gpsParams = new LinkedHashMap<>();
        gpsParams.put("type", "normal_walk");
        gpsParams.put("duration", 300);
        instructions.add(new BasicTestInstruction(
                "normal_walk",
                TestAction.GENERATE_GPS_TRAJECTORY,
                gpsParams,
                0L,
                SafetyLevel.SAFE_SIMULATION
        ));
        instructions.add(new BasicTestInstruction(
                "wait_before_jump",
                TestAction.WAIT_FOR_DURATION,
                new LinkedHashMap<>(),
                120000L,
                SafetyLevel.SAFE_SIMULATION
        ));
        Map<String, Object> jumpParams = new LinkedHashMap<>();
        jumpParams.put("jump_distance", 1000);
        jumpParams.put("duration", 5);
        instructions.add(new BasicTestInstruction(
                "gps_jump",
                TestAction.SIMULATE_GPS_JUMP,
                jumpParams,
                0L,
                SafetyLevel.SAFE_SIMULATION
        ));
        return new TestScenario("anomaly_gps_jump", "异常-定位跳跃", "测试GPS突然跳跃的检测能力", "anomaly", instructions);
    }
}
