package com.acooldog.toolbox.algorithmkit.scenario;

import com.acooldog.toolbox.algorithmkit.instruction.dsl.TestInstruction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TestScenario {
    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final List<TestInstruction> instructions;

    public TestScenario(String id, String name, String description, String category, List<TestInstruction> instructions) {
        this.id = normalize(id, "scenario_" + System.currentTimeMillis());
        this.name = normalize(name, "未命名测试场景");
        this.description = description == null ? "" : description.trim();
        this.category = normalize(category, "custom");
        this.instructions = Collections.unmodifiableList(new ArrayList<>(instructions == null ? Collections.emptyList() : instructions));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public List<TestInstruction> getInstructions() {
        return instructions;
    }

    public TestScenario withInstructions(List<TestInstruction> nextInstructions) {
        return new TestScenario(id, name, description, category, nextInstructions);
    }

    private String normalize(String raw, String fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        return raw.trim();
    }
}
