package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RootFeatureRuntimeReport {
    public enum State {
        DISABLED,
        LOADED,
        BLOCKED
    }

    private final int configVersion;
    private final long reloadedAtMillis;
    private final EnumMap<RootFeature, State> states;
    private final EnumMap<RootFeature, String> messages;

    RootFeatureRuntimeReport(
            int configVersion,
            long reloadedAtMillis,
            @NonNull Map<RootFeature, State> states,
            @NonNull Map<RootFeature, String> messages
    ) {
        this.configVersion = configVersion;
        this.reloadedAtMillis = reloadedAtMillis;
        this.states = new EnumMap<>(RootFeature.class);
        this.messages = new EnumMap<>(RootFeature.class);
        for (RootFeature feature : RootFeature.values()) {
            this.states.put(feature, states.get(feature) == null ? State.DISABLED : states.get(feature));
            this.messages.put(feature, messages.get(feature) == null ? "" : messages.get(feature));
        }
    }

    public int getConfigVersion() {
        return configVersion;
    }

    public long getReloadedAtMillis() {
        return reloadedAtMillis;
    }

    @NonNull
    public State getState(@NonNull RootFeature feature) {
        State state = states.get(feature);
        return state == null ? State.DISABLED : state;
    }

    @NonNull
    public String getMessage(@NonNull RootFeature feature) {
        String message = messages.get(feature);
        return message == null ? "" : message;
    }

    @NonNull
    public List<String> summarizeLines() {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.getDefault(), "配置版本：%d，热重载时间：%d", configVersion, reloadedAtMillis));
        for (RootFeature feature : RootFeature.values()) {
            State state = getState(feature);
            if (state == State.LOADED || state == State.BLOCKED) {
                lines.add(feature.getConfigKey() + "："
                        + (state == State.LOADED ? "已加载" : "已阻断")
                        + " - " + getMessage(feature));
            }
        }
        if (lines.size() == 1) {
            lines.add("未开启任何Root测试能力。");
        }
        return Collections.unmodifiableList(lines);
    }
}
