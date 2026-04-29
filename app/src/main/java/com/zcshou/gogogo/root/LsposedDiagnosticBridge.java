package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LsposedDiagnosticBridge {
    public static final String LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager";
    public static final String ACTION_DIAGNOSTIC_CONTROL = "com.acooldog.toolbox.root.DIAGNOSTIC_CONTROL";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_MODULE_IDS = "moduleIds";
    public static final String EXTRA_SETTINGS_JSON = "settingsJson";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String SCOPE_TARGET_LABEL = "LSPosed作用域目标";

    private LsposedDiagnosticBridge() {
        // Utility class.
    }

    public static boolean isManagerInstalled(@NonNull Context context) {
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        try {
            packageManager.getPackageInfo(LSPOSED_MANAGER_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean openManager(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(LSPOSED_MANAGER_PACKAGE);
        if (intent == null) {
            return false;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }

    public static void broadcastStart(
            @NonNull Context context,
            @NonNull String sessionId,
            @NonNull List<RootDiagnosticModule> modules,
            @NonNull RootDiagnosticSettings settings
    ) {
        Intent intent = baseIntent(COMMAND_START);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.putExtra(EXTRA_MODULE_IDS, moduleIds(modules));
        intent.putExtra(EXTRA_SETTINGS_JSON, settings.toJson());
        context.getApplicationContext().sendBroadcast(intent);
    }

    public static void broadcastStop(@NonNull Context context, @NonNull String sessionId) {
        Intent intent = baseIntent(COMMAND_STOP);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        context.getApplicationContext().sendBroadcast(intent);
    }

    public static boolean hasModule(@Nullable String[] moduleIds, @NonNull RootDiagnosticModule module) {
        if (moduleIds == null) {
            return false;
        }
        for (String moduleId : moduleIds) {
            if (module.getId().equals(moduleId)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static List<RootDiagnosticModule> parseModules(@Nullable String[] moduleIds) {
        List<RootDiagnosticModule> modules = new ArrayList<>();
        if (moduleIds == null) {
            return modules;
        }
        for (String moduleId : moduleIds) {
            RootDiagnosticModule module = RootDiagnosticModule.fromId(moduleId);
            if (module != null) {
                modules.add(module);
            }
        }
        return modules;
    }

    @NonNull
    private static Intent baseIntent(@NonNull String command) {
        Intent intent = new Intent(ACTION_DIAGNOSTIC_CONTROL);
        intent.putExtra(EXTRA_COMMAND, command);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return intent;
    }

    @NonNull
    private static String[] moduleIds(@NonNull List<RootDiagnosticModule> modules) {
        List<String> ids = new ArrayList<>();
        for (RootDiagnosticModule module : modules) {
            if (module != null && !TextUtils.isEmpty(module.getId())) {
                ids.add(module.getId());
            }
        }
        return ids.toArray(new String[0]);
    }
}
