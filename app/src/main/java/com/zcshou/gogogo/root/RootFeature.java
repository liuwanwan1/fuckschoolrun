package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public enum RootFeature {
    ENVIRONMENT_INSPECTION("environment_inspection", false),
    ROOT_SHELL_PROBE("root_shell_probe", false),
    ENCRYPTED_AUDIT_LOG("encrypted_audit_log", false),
    GM_TEST_INTERFACE("gm_test_interface", false),
    FRIDA_DYNAMIC_INJECTION("frida_dynamic_injection", true),
    ROOT_NMEA_INJECTION("root_nmea_injection", true),
    SIGNAL_SIMULATION("signal_simulation", true),
    MOCK_LOCATION_BYPASS("mock_location_bypass", true),
    TARGET_APP_HOOK("target_app_hook", true),
    SYSTEM_SERVICE_STREAM_LOG("system_service_stream_log", true),
    SENSOR_EVENT_INJECTION("sensor_event_injection", true);

    private final String configKey;
    private final boolean restrictedExecution;

    RootFeature(@NonNull String configKey, boolean restrictedExecution) {
        this.configKey = configKey;
        this.restrictedExecution = restrictedExecution;
    }

    @NonNull
    public String getConfigKey() {
        return configKey;
    }

    public boolean isRestrictedExecution() {
        return restrictedExecution;
    }

    @Nullable
    public static RootFeature fromConfigKey(@Nullable String configKey) {
        if (configKey == null) {
            return null;
        }
        for (RootFeature feature : values()) {
            if (feature.configKey.equals(configKey)) {
                return feature;
            }
        }
        return null;
    }
}
