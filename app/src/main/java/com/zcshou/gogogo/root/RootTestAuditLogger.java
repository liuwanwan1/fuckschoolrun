package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;

import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RootTestAuditLogger {
    private static final String PREFS_NAME = "root_test_audit_store";
    private static final String KEY_EVENTS = "events";
    private static final int MAX_EVENTS = 40;

    private final SharedPreferences preferences;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());

    public RootTestAuditLogger(@NonNull Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void append(@NonNull String event) {
        String entry = timestampFormat.format(new Date()) + " " + event;
        List<String> entries = getRecentEntries();
        entries.add(0, entry);
        while (entries.size() > MAX_EVENTS) {
            entries.remove(entries.size() - 1);
        }
        JSONArray array = new JSONArray();
        for (String item : entries) {
            array.put(item);
        }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply();
        XLog.i("RootTestAudit: " + event);
    }

    @NonNull
    public List<String> getRecentEntries() {
        List<String> entries = new ArrayList<>();
        String raw = preferences.getString(KEY_EVENTS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int index = 0; index < array.length(); index++) {
                String item = array.optString(index, "");
                if (!item.trim().isEmpty()) {
                    entries.add(item);
                }
            }
        } catch (Exception ignored) {
            // Ignore corrupted local audit cache.
        }
        return entries;
    }
}
