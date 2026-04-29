package com.acooldog.toolbox.root;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class RootEnvironmentInspector {
    private static final long ROOT_PROBE_TIMEOUT_SECONDS = 8L;

    private static final String[] ROOT_MANAGER_PACKAGES = new String[] {
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk",
            "me.weishu.kernelsu",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser"
    };

    private static final String[] HOOK_FRAMEWORK_PACKAGES = new String[] {
            "org.lsposed.manager",
            "de.robv.android.xposed.installer",
            "org.meowcat.edxposed.manager",
            "me.weishu.exp",
            "com.saurik.substrate",
            "top.canyie.pine"
    };

    private static final String[] SU_BINARY_PATHS = new String[] {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/su/bin/su",
            "/vendor/bin/su",
            "/system/bin/.ext/su",
            "/system/usr/we-need-root/su"
    };

    private final Context appContext;

    public RootEnvironmentInspector(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    public RootEnvironmentReport inspect() {
        List<String> rootManagers = detectInstalledPackages(ROOT_MANAGER_PACKAGES);
        List<String> hookFrameworks = detectInstalledPackages(HOOK_FRAMEWORK_PACKAGES);
        List<String> suPaths = detectExistingPaths(SU_BINARY_PATHS);
        boolean developerOptionsEnabled = isDeveloperOptionsEnabled();
        boolean mockLocationAllowed = isCurrentAppMockLocationAllowed();
        boolean legacyMockLocationEnabled = isLegacyMockLocationEnabled();
        boolean hiddenRootLikely = !rootManagers.isEmpty() && suPaths.isEmpty();
        return new RootEnvironmentReport(
                rootManagers,
                suPaths,
                hookFrameworks,
                developerOptionsEnabled,
                mockLocationAllowed,
                legacyMockLocationEnabled,
                hiddenRootLikely,
                System.currentTimeMillis()
        );
    }

    @NonNull
    public RootShellProbeResult requestRootShellProbe() {
        java.lang.Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] {"su", "-c", "id"});
            boolean finished = process.waitFor(ROOT_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return new RootShellProbeResult(false, true, -1, "su probe timed out");
            }
            String output = (readFully(process.getInputStream()) + readFully(process.getErrorStream())).trim();
            int exitCode = process.exitValue();
            return new RootShellProbeResult(exitCode == 0, false, exitCode, output);
        } catch (Exception exception) {
            return new RootShellProbeResult(false, false, -1, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @NonNull
    private List<String> detectInstalledPackages(@NonNull String[] packageNames) {
        List<String> installed = new ArrayList<>();
        PackageManager packageManager = appContext.getPackageManager();
        for (String packageName : packageNames) {
            try {
                packageManager.getPackageInfo(packageName, 0);
                installed.add(packageName);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Treat missing or package-visibility filtered packages as not detected.
            } catch (Exception ignored) {
                // Keep diagnostics best-effort only.
            }
        }
        return installed;
    }

    @NonNull
    private List<String> detectExistingPaths(@NonNull String[] paths) {
        List<String> existing = new ArrayList<>();
        for (String path : paths) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    existing.add(path);
                }
            } catch (Exception ignored) {
                // Keep diagnostics best-effort only.
            }
        }
        return existing;
    }

    private boolean isDeveloperOptionsEnabled() {
        try {
            return Settings.Global.getInt(
                    appContext.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                    0
            ) == 1;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isCurrentAppMockLocationAllowed() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return isLegacyMockLocationEnabled();
        }
        AppOpsManager appOpsManager = (AppOpsManager) appContext.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsManager == null) {
            return false;
        }
        try {
            int mode;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mode = appOpsManager.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        appContext.getPackageName()
                );
            } else {
                mode = appOpsManager.checkOpNoThrow(
                        AppOpsManager.OPSTR_MOCK_LOCATION,
                        Process.myUid(),
                        appContext.getPackageName()
                );
            }
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isLegacyMockLocationEnabled() {
        try {
            return Settings.Secure.getInt(appContext.getContentResolver(), "mock_location", 0) != 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    @NonNull
    private String readFully(@NonNull InputStream stream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
