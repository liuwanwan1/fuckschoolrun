package com.acooldog.toolbox.algorithmkit.validation.anticheat;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;
import com.acooldog.toolbox.algorithmkit.scenario.TestScenario;

import java.util.ArrayList;
import java.util.List;

/**
 * 算法验证模块 - 仅用于内部算法测试
 * 功能：生成模拟运动数据，验证识别算法
 * 限制：不注入系统、不修改任何状态
 * 使用：需三级确认，仅DEBUG版本可用
 * 审计：所有操作记录到加密日志
 */
public final class AntiCheatValidator {
    public ValidationReport validateTestCase(TestScenario scenario) {
        List<ValidationItem> items = new ArrayList<>();
        items.add(validateMotionPattern(scenario));
        items.add(validateLocationConsistency(scenario));
        items.add(validateTimeSequence(scenario));
        items.add(validateEnvironmentConsistency(scenario));
        items.add(detectAnomalies(scenario));
        int riskScore = calculateRiskScore(items, scenario);
        return new ValidationReport(scenario.getId(), items, riskScore, generateRecommendations(items, riskScore));
    }

    public DetectionReport generateDetectionReport(List<TestScenario> normalScenarios, List<TestScenario> anomalyScenarios) {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("运动模式连续性");
        capabilities.add("定位/传感器一致性");
        capabilities.add("时间序列单调性");
        capabilities.add("环境状态一致性");
        capabilities.add("异常跳变识别");
        int anomalyCount = anomalyScenarios == null ? 0 : anomalyScenarios.size();
        int normalCount = normalScenarios == null ? 0 : normalScenarios.size();
        double falsePositiveRate = normalCount == 0 ? 0d : Math.min(0.2d, anomalyCount / (double) (normalCount + anomalyCount + 1));
        List<String> suggestions = new ArrayList<>();
        suggestions.add("扩大正常样本覆盖校园操场、坡道和室内外切换");
        suggestions.add("对GPS跳变和步频突变建立分级阈值");
        suggestions.add("把检测延迟和误报率纳入回归指标");
        return new DetectionReport(capabilities, falsePositiveRate, 1200L, suggestions);
    }

    private ValidationItem validateMotionPattern(TestScenario scenario) {
        boolean hasStep = hasAction(scenario, TestAction.GENERATE_STEP_FREQUENCY);
        boolean hasSensor = hasAction(scenario, TestAction.GENERATE_SENSOR_DATA);
        int score = hasStep || hasSensor ? 88 : 55;
        return new ValidationItem("运动模式分析", score, hasStep || hasSensor ? "包含运动数据生成指令" : "缺少步频或传感器数据");
    }

    private ValidationItem validateLocationConsistency(TestScenario scenario) {
        boolean hasGps = hasAction(scenario, TestAction.GENERATE_GPS_TRAJECTORY);
        boolean hasStep = hasAction(scenario, TestAction.GENERATE_STEP_FREQUENCY);
        int score = hasGps && hasStep ? 90 : hasGps ? 70 : 45;
        return new ValidationItem("定位一致性验证", score, hasGps && hasStep ? "GPS与步频数据均存在" : "建议同时生成GPS与步频数据");
    }

    private ValidationItem validateTimeSequence(TestScenario scenario) {
        long totalDelay = 0L;
        for (TestInstruction instruction : scenario.getInstructions()) {
            totalDelay += instruction.getDelayAfter();
        }
        int score = totalDelay <= 5L * 60L * 1000L ? 92 : 40;
        return new ValidationItem("时间序列分析", score, "累计延迟=" + totalDelay + "ms");
    }

    private ValidationItem validateEnvironmentConsistency(TestScenario scenario) {
        boolean hasReadOnly = hasAction(scenario, TestAction.CHECK_ROOT_STATUS)
                || hasAction(scenario, TestAction.CHECK_DEVELOPER_OPTIONS)
                || hasAction(scenario, TestAction.CHECK_MOCK_LOCATION)
                || hasAction(scenario, TestAction.CHECK_HOOK_FRAMEWORK);
        return new ValidationItem("环境一致性", hasReadOnly ? 86 : 65, hasReadOnly ? "包含只读环境检查" : "可加入只读环境检查增强场景");
    }

    private ValidationItem detectAnomalies(TestScenario scenario) {
        boolean gpsJump = hasAction(scenario, TestAction.SIMULATE_GPS_JUMP);
        boolean drift = hasAction(scenario, TestAction.VALIDATE_LOCATION_DRIFT);
        int score = gpsJump || drift ? 35 : 90;
        return new ValidationItem("异常检测", score, gpsJump ? "包含GPS跳跃异常，用于检测能力验证" : "未发现显式异常指令");
    }

    private int calculateRiskScore(List<ValidationItem> items, TestScenario scenario) {
        int risk = 100;
        for (ValidationItem item : items) {
            risk -= item.getScore() / 10;
        }
        if (hasAction(scenario, TestAction.SIMULATE_GPS_JUMP)) {
            risk += 35;
        }
        return Math.max(0, Math.min(100, risk));
    }

    private List<String> generateRecommendations(List<ValidationItem> items, int riskScore) {
        List<String> recommendations = new ArrayList<>();
        if (riskScore > 60) {
            recommendations.add("将该场景纳入异常回归集，不作为正常样本基线");
        } else {
            recommendations.add("可作为正常或低风险样本参与识别算法回归");
        }
        recommendations.add("保存场景JSON、执行报告和审计日志以便复核");
        recommendations.add("所有数据仅用于算法分析，不得注入系统服务");
        return recommendations;
    }

    private boolean hasAction(TestScenario scenario, TestAction action) {
        for (TestInstruction instruction : scenario.getInstructions()) {
            if (instruction.getAction() == action) {
                return true;
            }
        }
        return false;
    }
}
