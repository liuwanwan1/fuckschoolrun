package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class RootDiagnosticSettingsStore {
    private static final String PREFS_NAME = "root_diagnostic_settings_store";
    private static final String KEY_SETTINGS_JSON = "settings_json";

    private final SharedPreferences preferences;

    public RootDiagnosticSettingsStore(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    public RootDiagnosticSettings load() {
        return RootDiagnosticSettings.fromJson(preferences.getString(KEY_SETTINGS_JSON, ""));
    }

    public void save(@NonNull RootDiagnosticSettings settings) {
        preferences.edit()
                .putString(KEY_SETTINGS_JSON, settings.toJson())
                .apply();
    }
}
