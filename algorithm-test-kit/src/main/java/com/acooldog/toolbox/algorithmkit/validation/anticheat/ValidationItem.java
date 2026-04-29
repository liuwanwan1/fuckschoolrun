package com.acooldog.toolbox.algorithmkit.validation.anticheat;

public final class ValidationItem {
    private final String name;
    private final int score;
    private final String message;

    public ValidationItem(String name, int score, String message) {
        this.name = name;
        this.score = Math.max(0, Math.min(100, score));
        this.message = message == null ? "" : message;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public String getMessage() {
        return message;
    }
}
