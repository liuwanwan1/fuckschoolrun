package com.acooldog.toolbox.algorithmkit.scenario;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.BasicTestInstruction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.SafetyLevel;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestAction;
import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ScenarioSerializer {
    private ScenarioSerializer() {
    }

    public static String toJson(TestScenario scenario) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"id\":\"").append(escape(scenario.getId())).append("\",")
                .append("\"name\":\"").append(escape(scenario.getName())).append("\",")
                .append("\"description\":\"").append(escape(scenario.getDescription())).append("\",")
                .append("\"category\":\"").append(escape(scenario.getCategory())).append("\",")
                .append("\"instructions\":[");
        List<TestInstruction> instructions = scenario.getInstructions();
        for (int index = 0; index < instructions.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            appendInstruction(builder, instructions.get(index));
        }
        builder.append("]}");
        return builder.toString();
    }

    public static String toYaml(TestScenario scenario) {
        StringBuilder builder = new StringBuilder();
        builder.append("id: ").append(scenario.getId()).append('\n')
                .append("name: ").append(scenario.getName()).append('\n')
                .append("description: ").append(scenario.getDescription()).append('\n')
                .append("category: ").append(scenario.getCategory()).append('\n')
                .append("instructions:\n");
        for (TestInstruction instruction : scenario.getInstructions()) {
            builder.append("  - id: ").append(instruction.getId()).append('\n')
                    .append("    action: ").append(instruction.getAction().name()).append('\n')
                    .append("    safetyLevel: ").append(instruction.getSafetyLevel().name()).append('\n')
                    .append("    delayAfter: ").append(instruction.getDelayAfter()).append('\n')
                    .append("    parameters:\n");
            for (Map.Entry<String, Object> entry : instruction.getParameters().entrySet()) {
                builder.append("      ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
        }
        return builder.toString();
    }

    public static TestScenario fromSimpleJson(String raw) {
        String id = readString(raw, "id", "scenario_" + System.currentTimeMillis());
        String name = readString(raw, "name", "未命名测试场景");
        String description = readString(raw, "description", "");
        String category = readString(raw, "category", "custom");
        List<TestInstruction> instructions = new ArrayList<>();
        String array = readArray(raw, "instructions");
        for (String object : splitObjects(array)) {
            instructions.add(readInstruction(object));
        }
        return new TestScenario(id, name, description, category, instructions);
    }

    private static void appendInstruction(StringBuilder builder, TestInstruction instruction) {
        builder.append("{\"id\":\"").append(escape(instruction.getId())).append("\",")
                .append("\"action\":\"").append(instruction.getAction().name()).append("\",")
                .append("\"delayAfter\":").append(instruction.getDelayAfter()).append(',')
                .append("\"safetyLevel\":\"").append(instruction.getSafetyLevel().name()).append("\",")
                .append("\"parameters\":{");
        int index = 0;
        for (Map.Entry<String, Object> entry : instruction.getParameters().entrySet()) {
            if (index++ > 0) {
                builder.append(',');
            }
            builder.append("\"").append(escape(entry.getKey())).append("\":");
            appendJsonValue(builder, entry.getValue());
        }
        builder.append("}}");
    }

    private static TestInstruction readInstruction(String raw) {
        String id = readString(raw, "id", "instruction_" + System.currentTimeMillis());
        TestAction action = TestAction.valueOf(readString(raw, "action", "WAIT_FOR_DURATION"));
        SafetyLevel safetyLevel = SafetyLevel.valueOf(readString(raw, "safetyLevel", "SAFE_SIMULATION"));
        long delayAfter = readLong(raw, "delayAfter", 0L);
        Map<String, Object> parameters = new LinkedHashMap<>();
        String params = readObject(raw, "parameters");
        for (String pair : splitPairs(params)) {
            int colon = pair.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = unquote(pair.substring(0, colon).trim());
            String value = pair.substring(colon + 1).trim();
            parameters.put(key, parseValue(value));
        }
        return new BasicTestInstruction(id, action, parameters, delayAfter, safetyLevel);
    }

    private static void appendJsonValue(StringBuilder builder, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            builder.append("\"").append(escape(String.valueOf(value))).append("\"");
        }
    }

    private static String readString(String raw, String key, String fallback) {
        String value = readRawValue(raw, key);
        if (value == null) {
            return fallback;
        }
        return unquote(value.trim());
    }

    private static long readLong(String raw, String key, long fallback) {
        try {
            String value = readRawValue(raw, key);
            return value == null ? fallback : Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String readArray(String raw, String key) {
        return readEnclosed(raw, key, '[', ']');
    }

    private static String readObject(String raw, String key) {
        return readEnclosed(raw, key, '{', '}');
    }

    private static String readRawValue(String raw, String key) {
        int keyIndex = raw == null ? -1 : raw.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return null;
        }
        int colon = raw.indexOf(':', keyIndex);
        if (colon < 0) {
            return null;
        }
        int start = colon + 1;
        while (start < raw.length() && Character.isWhitespace(raw.charAt(start))) {
            start++;
        }
        if (start < raw.length() && raw.charAt(start) == '"') {
            int end = start + 1;
            while (end < raw.length() && raw.charAt(end) != '"') {
                end++;
            }
            return raw.substring(start, Math.min(end + 1, raw.length()));
        }
        int end = start;
        while (end < raw.length() && raw.charAt(end) != ',' && raw.charAt(end) != '}' && raw.charAt(end) != ']') {
            end++;
        }
        return raw.substring(start, end);
    }

    private static String readEnclosed(String raw, String key, char open, char close) {
        int keyIndex = raw == null ? -1 : raw.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return "";
        }
        int start = raw.indexOf(open, keyIndex);
        if (start < 0) {
            return "";
        }
        int depth = 0;
        for (int index = start; index < raw.length(); index++) {
            char item = raw.charAt(index);
            if (item == open) {
                depth++;
            } else if (item == close) {
                depth--;
                if (depth == 0) {
                    return raw.substring(start + 1, index);
                }
            }
        }
        return "";
    }

    private static List<String> splitObjects(String raw) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int index = 0; index < raw.length(); index++) {
            char item = raw.charAt(index);
            if (item == '{') {
                if (depth == 0) {
                    start = index;
                }
                depth++;
            } else if (item == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(raw.substring(start, index + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private static List<String> splitPairs(String raw) {
        List<String> pairs = new ArrayList<>();
        int start = 0;
        boolean quoted = false;
        for (int index = 0; index < raw.length(); index++) {
            char item = raw.charAt(index);
            if (item == '"') {
                quoted = !quoted;
            } else if (item == ',' && !quoted) {
                pairs.add(raw.substring(start, index));
                start = index + 1;
            }
        }
        if (start < raw.length()) {
            pairs.add(raw.substring(start));
        }
        return pairs;
    }

    private static Object parseValue(String raw) {
        String value = raw.trim();
        if (value.startsWith("\"")) {
            return unquote(value);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            }
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static String unquote(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return trimmed;
    }

    private static String escape(String raw) {
        return raw == null ? "" : raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
