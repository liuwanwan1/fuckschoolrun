package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LsposedDiagnosticBridge {
    public static final String LSPOSED_MANAGER_PACKAGE = "org.lsposed.manager";
    public static final String LSPOSED_PARASITIC_PACKAGE = "com.android.shell";
    public static final String LSPOSED_PARASITIC_CATEGORY = "org.lsposed.manager.LAUNCH_MANAGER";
    public static final String LSPOSED_PARASITIC_INDICATOR = "com.android.shell:LSPosed寄生管理器";
    public static final String ACTION_DIAGNOSTIC_CONTROL = "com.acooldog.toolbox.root.DIAGNOSTIC_CONTROL";
    public static final String ACTION_DIAGNOSTIC_STATE_REQUEST = "com.acooldog.toolbox.root.DIAGNOSTIC_STATE_REQUEST";
    public static final String EXTRA_COMMAND = "command";
    public static final String EXTRA_SESSION_ID = "sessionId";
    public static final String EXTRA_MODULE_IDS = "moduleIds";
    public static final String EXTRA_SETTINGS_JSON = "settingsJson";
    public static final String EXTRA_TARGET_PACKAGE = "targetPackage";
    public static final String COMMAND_START = "start";
    public static final String COMMAND_STOP = "stop";
    public static final String COMMAND_UPDATE_LOCATION = "update_location";
    public static final String COMMAND_UPDATE_SETTINGS = "update_settings";
    public static final String SCOPE_TARGET_LABEL = "LSPosed作用域目标";
    private static final String[] MANAGER_PACKAGES = new String[] {
            LSPOSED_MANAGER_PACKAGE,
            "io.github.libxposed.manager",
            "org.lsposed.lspd"
    };

    private LsposedDiagnosticBridge() {
        // Utility class.
    }

    public static boolean isParasiticManagerAvailable(@NonNull Context context) {
        return findParasiticManagerIntent(context) != null;
    }

    @NonNull
    public static String describeManagerState(@NonNull Context context) {
        Intent directIntent = findDirectManagerIntent(context);
        if (directIntent != null) {
            return "可打开独立管理器快捷入口";
        }
        if (isParasiticManagerAvailable(context)) {
            return "可打开 com.android.shell 寄生管理器入口";
        }
        if (isKnownManagerPackageInstalled(context)) {
            return "检测到旧管理器包，但未找到可启动入口";
        }
        return "不再检测管理器APK；目标进程启动后由LSPosed加载模块";
    }

    private static boolean isKnownManagerPackageInstalled(@NonNull Context context) {
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        for (String packageName : MANAGER_PACKAGES) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                return true;
            } catch (PackageManager.NameNotFoundException ignored) {
                // Try the next known manager package.
            } catch (Exception ignored) {
                // Keep detection best-effort.
            }
        }
        return false;
    }

    public static boolean openManager(@NonNull Context context) {
        Intent intent = findManagerLaunchIntent(context);
        if (intent == null) {
            return false;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }

    @Nullable
    private static Intent findManagerLaunchIntent(@NonNull Context context) {
        Intent directIntent = findDirectManagerIntent(context);
        if (directIntent != null) {
            return directIntent;
        }
        return findParasiticManagerIntent(context);
    }

    @Nullable
    private static Intent findDirectManagerIntent(@NonNull Context context) {
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        for (String packageName : MANAGER_PACKAGES) {
            try {
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                if (intent != null) {
                    return intent;
                }
            } catch (Exception ignored) {
                // Try the next manager package.
            }
        }
        return null;
    }

    @Nullable
    private static Intent findParasiticManagerIntent(@NonNull Context context) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(LSPOSED_PARASITIC_CATEGORY);
        intent.setPackage(LSPOSED_PARASITIC_PACKAGE);
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        try {
            ResolveInfo resolveInfo = packageManager.resolveActivity(intent, 0);
            return resolveInfo == null ? null : intent;
        } catch (Exception ignored) {
            return null;
        }
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

    public static void broadcastUpdateLocation(
            @NonNull Context context,
            @NonNull String sessionId,
            @NonNull RootDiagnosticSettings settings
    ) {
        Intent intent = baseIntent(COMMAND_UPDATE_LOCATION);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.putExtra(EXTRA_SETTINGS_JSON, settings.toJson());
        context.getApplicationContext().sendBroadcast(intent);
    }

    public static void broadcastUpdateSettings(
            @NonNull Context context,
            @NonNull String sessionId,
            @NonNull RootDiagnosticSettings settings
    ) {
        Intent intent = baseIntent(COMMAND_UPDATE_SETTINGS);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.putExtra(EXTRA_SETTINGS_JSON, settings.toJson());
        context.getApplicationContext().sendBroadcast(intent);
    }

    public static void broadcastStateRequest(@NonNull Context context, @NonNull String targetPackageName) {
        Intent intent = new Intent(ACTION_DIAGNOSTIC_STATE_REQUEST);
        intent.putExtra(EXTRA_TARGET_PACKAGE, targetPackageName);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
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
