package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class RootDiagnosticEvent {
    public static final String MODULE_FRAMEWORK = "framework";
    public static final String MODULE_REPORT = "report";
    public static final String FRIDA_PREFIX = "[SchoolRunDiag]";

    private final long timestampMillis;
    private final String sessionId;
    private final String targetPackageName;
    private final String moduleId;
    private final String type;
    private final String detail;

    public RootDiagnosticEvent(
            long timestampMillis,
            @NonNull String sessionId,
            @NonNull String targetPackageName,
            @NonNull String moduleId,
            @NonNull String type,
            @NonNull String detail
    ) {
        this.timestampMillis = timestampMillis <= 0L ? System.currentTimeMillis() : timestampMillis;
        this.sessionId = sessionId;
        this.targetPackageName = targetPackageName;
        this.moduleId = moduleId;
        this.type = type;
        this.detail = detail;
    }

    @NonNull
    public static RootDiagnosticEvent local(
            @NonNull String sessionId,
            @NonNull String targetPackageName,
            @NonNull String moduleId,
            @NonNull String type,
            @NonNull String detail
    ) {
        return new RootDiagnosticEvent(
                System.currentTimeMillis(),
                sessionId,
                targetPackageName,
                moduleId,
                type,
                detail
        );
    }

    @Nullable
    public static RootDiagnosticEvent fromFridaLine(
            @NonNull String fallbackSessionId,
            @NonNull String fallbackTargetPackageName,
            @NonNull String line
    ) {
        int prefixIndex = line.indexOf(FRIDA_PREFIX);
        if (prefixIndex < 0) {
            return null;
        }
        String rawJson = line.substring(prefixIndex + FRIDA_PREFIX.length()).trim();
        try {
            JSONObject object = new JSONObject(rawJson);
            return new RootDiagnosticEvent(
                    object.optLong("at", System.currentTimeMillis()),
                    object.optString("sessionId", fallbackSessionId),
                    object.optString("target", fallbackTargetPackageName),
                    object.optString("module", MODULE_FRAMEWORK),
                    object.optString("type", "frida"),
                    object.optString("detail", rawJson)
            );
        } catch (Exception ignored) {
            return local(
                    fallbackSessionId,
                    fallbackTargetPackageName,
                    MODULE_FRAMEWORK,
                    "frida_raw",
                    line
            );
        }
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public String getTargetPackageName() {
        return targetPackageName;
    }

    @NonNull
    public String getModuleId() {
        return moduleId;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @NonNull
    public String getDetail() {
        return detail;
    }

    public boolean belongsToModule(@NonNull RootDiagnosticModule module) {
        return module.getId().equals(moduleId);
    }

    public boolean containsSignal(@NonNull String token) {
        String lowerToken = token.toLowerCase(Locale.US);
        return type.toLowerCase(Locale.US).contains(lowerToken)
                || detail.toLowerCase(Locale.US).contains(lowerToken);
    }

    @NonNull
    public String toDisplayLine() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return format.format(new Date(timestampMillis))
                + " [" + moduleId + "/" + type + "] "
                + detail;
    }

    @NonNull
    public JSONObject toJsonObject() {
        JSONObject object = new JSONObject();
        try {
            object.put("timestampMillis", timestampMillis);
            object.put("sessionId", sessionId);
            object.put("targetPackageName", targetPackageName);
            object.put("moduleId", moduleId);
            object.put("type", type);
            object.put("detail", detail);
        } catch (Exception ignored) {
            // JSONObject only fails on invalid numeric values; keep best-effort output.
        }
        return object;
    }
}
