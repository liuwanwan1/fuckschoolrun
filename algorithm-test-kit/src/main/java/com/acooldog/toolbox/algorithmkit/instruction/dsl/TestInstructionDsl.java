package com.acooldog.toolbox.algorithmkit.instruction.dsl;

import com.acooldog.toolbox.algorithmkit.scenario.TestScenario;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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
public final class TestInstructionDsl {
    private TestInstructionDsl() {
    }

    public static String toDsl(TestScenario scenario) {
        StringBuilder builder = new StringBuilder();
        builder.append("scenario ")
                .append(scenario.getId())
                .append(" name=\"")
                .append(escape(scenario.getName()))
                .append("\" category=")
                .append(scenario.getCategory())
                .append('\n');
        for (TestInstruction instruction : scenario.getInstructions()) {
            builder.append("instruction ")
                    .append(instruction.getId())
                    .append(" action=")
                    .append(instruction.getAction().name())
                    .append(" safety=")
                    .append(instruction.getSafetyLevel().name())
                    .append(" delay=")
                    .append(instruction.getDelayAfter());
            for (Map.Entry<String, Object> entry : instruction.getParameters().entrySet()) {
                builder.append(' ')
                        .append(entry.getKey())
                        .append('=')
                        .append(formatValue(entry.getValue()));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    public static TestScenario parse(String raw) {
        String scenarioId = "scenario_" + System.currentTimeMillis();
        String name = "未命名测试场景";
        String category = "custom";
        List<TestInstruction> instructions = new ArrayList<>();
        String[] lines = raw == null ? new String[0] : raw.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            if (trimmed.startsWith("scenario ")) {
                Map<String, String> tokens = parseTokens(trimmed.substring("scenario ".length()));
                String[] parts = trimmed.split("\\s+", 3);
                if (parts.length > 1) {
                    scenarioId = parts[1];
                }
                name = tokens.containsKey("name") ? tokens.get("name") : name;
                category = tokens.containsKey("category") ? tokens.get("category") : category;
            } else if (trimmed.startsWith("instruction ")) {
                instructions.add(parseInstruction(trimmed));
            } else {
                throw new IllegalArgumentException("无法识别的DSL语句: " + trimmed);
            }
        }
        return new TestScenario(scenarioId, name, "", category, instructions);
    }

    private static TestInstruction parseInstruction(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("指令缺少ID");
        }
        String id = parts[1];
        Map<String, String> tokens = parseTokens(parts.length > 2 ? parts[2] : "");
        TestAction action = TestAction.valueOf(tokens.getOrDefault("action", "WAIT_FOR_DURATION").toUpperCase(Locale.US));
        SafetyLevel safetyLevel = SafetyLevel.valueOf(tokens.getOrDefault("safety", "SAFE_SIMULATION").toUpperCase(Locale.US));
        long delay = parseLong(tokens.get("delay"), 0L);
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            String key = entry.getKey();
            if ("action".equals(key) || "safety".equals(key) || "delay".equals(key)) {
                continue;
            }
            parameters.put(key, coerce(entry.getValue()));
        }
        return new BasicTestInstruction(id, action, parameters, delay, safetyLevel);
    }

    private static Map<String, String> parseTokens(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        int index = 0;
        while (index < raw.length()) {
            while (index < raw.length() && Character.isWhitespace(raw.charAt(index))) {
                index++;
            }
            int keyStart = index;
            while (index < raw.length() && raw.charAt(index) != '=' && !Character.isWhitespace(raw.charAt(index))) {
                index++;
            }
            if (index >= raw.length() || raw.charAt(index) != '=') {
                while (index < raw.length() && !Character.isWhitespace(raw.charAt(index))) {
                    index++;
                }
                continue;
            }
            String key = raw.substring(keyStart, index);
            index++;
            String value;
            if (index < raw.length() && raw.charAt(index) == '"') {
                index++;
                int valueStart = index;
                while (index < raw.length() && raw.charAt(index) != '"') {
                    index++;
                }
                value = raw.substring(valueStart, Math.min(index, raw.length()));
                if (index < raw.length()) {
                    index++;
                }
            } else {
                int valueStart = index;
                while (index < raw.length() && !Character.isWhitespace(raw.charAt(index))) {
                    index++;
                }
                value = raw.substring(valueStart, index);
            }
            values.put(key, value);
        }
        return values;
    }

    private static Object coerce(String raw) {
        if (raw == null) {
            return "";
        }
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        try {
            if (raw.contains(".")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return raw == null ? fallback : Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String raw) {
        return raw == null ? "" : raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
