package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RootFeatureConfigStore {
    private static final String PREFS_NAME = "root_feature_config_store";
    private static final String KEY_CONFIG_JSON = "root_feature_config_json";
    private static final String CONFIG_FILE_NAME = "root_feature_config.json";

    private final Context appContext;
    private final SharedPreferences preferences;
    private final List<OnConfigChangedListener> listeners = new ArrayList<>();
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> {
                if (KEY_CONFIG_JSON.equals(key)) {
                    notifyConfigChanged(load());
                }
            };
    private boolean preferenceListenerRegistered;

    public RootFeatureConfigStore(@NonNull Context context) {
        appContext = context.getApplicationContext();
        preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ensureDefaultConfig();
    }

    @NonNull
    public RootFeatureConfig load() {
        String fileConfig = readConfigFile();
        if (!fileConfig.trim().isEmpty()) {
            return RootFeatureConfig.fromJson(fileConfig);
        }
        return RootFeatureConfig.fromJson(preferences.getString(KEY_CONFIG_JSON, ""));
    }

    public void save(@NonNull RootFeatureConfig config) {
        String configJson = config.toJson();
        writeConfigFile(configJson);
        preferences.edit()
                .putString(KEY_CONFIG_JSON, configJson)
                .apply();
    }

    @NonNull
    public RootFeatureConfig setFeature(@NonNull RootFeature feature, boolean enabled) {
        RootFeatureConfig config = load().withFeature(feature, enabled);
        save(config);
        return config;
    }

    @NonNull
    public RootFeatureConfig setTargetPackage(@NonNull String targetPackageName) {
        RootFeatureConfig config = load().withTargetPackage(targetPackageName);
        save(config);
        return config;
    }

    @NonNull
    public RootFeatureConfig setInjectionFramework(@NonNull RootFeatureConfig.InjectionFramework framework) {
        RootFeatureConfig config = load().withInjectionFramework(framework);
        save(config);
        return config;
    }

    public void registerListener(@NonNull OnConfigChangedListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
        if (!preferenceListenerRegistered) {
            preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
            preferenceListenerRegistered = true;
        }
    }

    public void unregisterListener(@NonNull OnConfigChangedListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty() && preferenceListenerRegistered) {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
            preferenceListenerRegistered = false;
        }
    }

    private void ensureDefaultConfig() {
        if (!getConfigFile().exists() && !preferences.contains(KEY_CONFIG_JSON)) {
            save(RootFeatureConfig.defaults());
        }
    }

    @NonNull
    public File getConfigFile() {
        return new File(appContext.getFilesDir(), CONFIG_FILE_NAME);
    }

    @NonNull
    private String readConfigFile() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return "";
        }
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            byte[] buffer = new byte[(int) Math.min(configFile.length(), 1024 * 64)];
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void writeConfigFile(@NonNull String configJson) {
        try (FileOutputStream outputStream = new FileOutputStream(getConfigFile(), false)) {
            outputStream.write(configJson.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // Keep SharedPreferences cache usable if the private config file cannot be written.
        }
    }

    private void notifyConfigChanged(@NonNull RootFeatureConfig config) {
        List<OnConfigChangedListener> snapshot = new ArrayList<>(listeners);
        for (OnConfigChangedListener listener : snapshot) {
            listener.onConfigChanged(config);
        }
    }

    public interface OnConfigChangedListener {
        void onConfigChanged(@NonNull RootFeatureConfig config);
    }
}
