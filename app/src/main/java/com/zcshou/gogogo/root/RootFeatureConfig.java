package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class RootFeatureConfig {
    private static final String KEY_VERSION = "version";
    private static final String KEY_UPDATED_AT = "updatedAt";
    private static final String KEY_INJECTION_FRAMEWORK = "injectionFramework";
    private static final String KEY_TARGET_PACKAGE = "targetPackage";
    private static final String KEY_FEATURES = "features";

    public enum InjectionFramework {
        NONE,
        FRIDA
    }

    private final int version;
    private final long updatedAtMillis;
    private final InjectionFramework injectionFramework;
    private final String targetPackageName;
    private final EnumMap<RootFeature, Boolean> switches;

    public RootFeatureConfig(
            int version,
            long updatedAtMillis,
            @NonNull InjectionFramework injectionFramework,
            @NonNull String targetPackageName,
            @NonNull Map<RootFeature, Boolean> switches
    ) {
        this.version = Math.max(1, version);
        this.updatedAtMillis = updatedAtMillis <= 0L ? System.currentTimeMillis() : updatedAtMillis;
        this.injectionFramework = injectionFramework;
        this.targetPackageName = targetPackageName.trim();
        this.switches = new EnumMap<>(RootFeature.class);
        for (RootFeature feature : RootFeature.values()) {
            this.switches.put(feature, Boolean.TRUE.equals(switches.get(feature)));
        }
    }

    @NonNull
    public static RootFeatureConfig defaults() {
        EnumMap<RootFeature, Boolean> defaults = new EnumMap<>(RootFeature.class);
        for (RootFeature feature : RootFeature.values()) {
            defaults.put(feature, false);
        }
        defaults.put(RootFeature.ENVIRONMENT_INSPECTION, true);
        defaults.put(RootFeature.ROOT_SHELL_PROBE, true);
        defaults.put(RootFeature.ENCRYPTED_AUDIT_LOG, true);
        return new RootFeatureConfig(
                1,
                System.currentTimeMillis(),
                InjectionFramework.FRIDA,
                "",
                defaults
        );
    }

    @NonNull
    public static RootFeatureConfig fromJson(@Nullable String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return defaults();
        }
        try {
            RootFeatureConfig fallback = defaults();
            JSONObject root = new JSONObject(rawJson);
            EnumMap<RootFeature, Boolean> switches = new EnumMap<>(RootFeature.class);
            for (RootFeature feature : RootFeature.values()) {
                switches.put(feature, fallback.isEnabled(feature));
            }
            JSONObject features = root.optJSONObject(KEY_FEATURES);
            if (features != null) {
                for (RootFeature feature : RootFeature.values()) {
                    if (features.has(feature.getConfigKey())) {
                        switches.put(feature, features.optBoolean(feature.getConfigKey(), false));
                    }
                }
            }
            return new RootFeatureConfig(
                    root.optInt(KEY_VERSION, 1),
                    root.optLong(KEY_UPDATED_AT, System.currentTimeMillis()),
                    parseInjectionFramework(root.optString(KEY_INJECTION_FRAMEWORK, InjectionFramework.FRIDA.name())),
                    root.optString(KEY_TARGET_PACKAGE, ""),
                    switches
            );
        } catch (Exception ignored) {
            return defaults();
        }
    }

    @NonNull
    public String toJson() {
        try {
            JSONObject root = new JSONObject();
            root.put(KEY_VERSION, version);
            root.put(KEY_UPDATED_AT, updatedAtMillis);
            root.put(KEY_INJECTION_FRAMEWORK, injectionFramework.name());
            root.put(KEY_TARGET_PACKAGE, targetPackageName);
            JSONObject features = new JSONObject();
            for (RootFeature feature : RootFeature.values()) {
                features.put(feature.getConfigKey(), isEnabled(feature));
            }
            root.put(KEY_FEATURES, features);
            return root.toString();
        } catch (Exception ignored) {
            return "{}";
        }
    }

    @NonNull
    public RootFeatureConfig withFeature(@NonNull RootFeature feature, boolean enabled) {
        EnumMap<RootFeature, Boolean> nextSwitches = new EnumMap<>(switches);
        nextSwitches.put(feature, enabled);
        return new RootFeatureConfig(
                version + 1,
                System.currentTimeMillis(),
                injectionFramework,
                targetPackageName,
                nextSwitches
        );
    }

    @NonNull
    public RootFeatureConfig withTargetPackage(@Nullable String targetPackageName) {
        return new RootFeatureConfig(
                version + 1,
                System.currentTimeMillis(),
                injectionFramework,
                targetPackageName == null ? "" : targetPackageName,
                switches
        );
    }

    public int getVersion() {
        return version;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    @NonNull
    public InjectionFramework getInjectionFramework() {
        return injectionFramework;
    }

    @NonNull
    public String getTargetPackageName() {
        return targetPackageName;
    }

    public boolean isEnabled(@NonNull RootFeature feature) {
        return Boolean.TRUE.equals(switches.get(feature));
    }

    @NonNull
    public EnumMap<RootFeature, Boolean> getSwitches() {
        return new EnumMap<>(switches);
    }

    @NonNull
    private static InjectionFramework parseInjectionFramework(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return InjectionFramework.FRIDA;
        }
        try {
            return InjectionFramework.valueOf(value.trim().toUpperCase(Locale.US));
        } catch (Exception ignored) {
            return InjectionFramework.FRIDA;
        }
    }
}
