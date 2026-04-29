package com.acooldog.toolbox.root;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RootDiagnosticCompatibilityCatalog {
    public static final String SOURCE_FUCK_RUN = "FUCK-RUN v1.0.6";

    private static final List<Profile> PROFILES;

    static {
        List<Profile> profiles = new ArrayList<>();
        profiles.add(new Profile("闪动校园", "com.huachenjie.shandong_school"));
        profiles.add(new Profile("闪动校园PRO", "com.huachenjie.shandong_school_pro"));
        profiles.add(new Profile("体适能", "com.bxkj.student"));
        profiles.add(new Profile("运动世界校园", "com.zjwh.android_wh_physicalfitness"));
        profiles.add(new Profile("宥马运动", "android.youma.com"));
        PROFILES = Collections.unmodifiableList(profiles);
    }

    private RootDiagnosticCompatibilityCatalog() {
        // Utility class.
    }

    @NonNull
    public static List<Profile> all() {
        return PROFILES;
    }

    @NonNull
    public static List<Profile> installedIn(@NonNull Context context) {
        PackageManager packageManager = context.getApplicationContext().getPackageManager();
        List<Profile> installed = new ArrayList<>();
        for (Profile profile : PROFILES) {
            try {
                packageManager.getPackageInfo(profile.getPackageName(), 0);
                installed.add(profile);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Not installed or hidden by package visibility.
            } catch (Exception ignored) {
                // Keep this catalog best-effort only.
            }
        }
        return installed;
    }

    @NonNull
    public static String summarizeForDisplay(@NonNull Context context) {
        List<Profile> installed = installedIn(context);
        StringBuilder builder = new StringBuilder();
        builder.append("公司内部适配画像：")
                .append(SOURCE_FUCK_RUN)
                .append("，已检测 ")
                .append(installed.size())
                .append('/')
                .append(PROFILES.size())
                .append(" 个我司内部传感器计步类软件包。");
        if (installed.isEmpty()) {
            builder.append("\n未在本机检测到画像中的内部软件；仍可通过 LSPosed 作用域选择其他内部授权目标。");
            return builder.toString();
        }
        builder.append("\n本机检测到：");
        for (int index = 0; index < installed.size(); index++) {
            Profile profile = installed.get(index);
            if (index > 0) {
                builder.append("、");
            }
            builder.append(profile.getDisplayName())
                    .append('(')
                    .append(profile.getPackageName())
                    .append(')');
        }
        builder.append("\n该画像仅用于我司内部授权测试核对，不会自动选择 LSPosed 作用域。");
        return builder.toString();
    }

    public static final class Profile {
        private final String displayName;
        private final String packageName;

        private Profile(@NonNull String displayName, @NonNull String packageName) {
            this.displayName = displayName;
            this.packageName = packageName;
        }

        @NonNull
        public String getDisplayName() {
            return displayName;
        }

        @NonNull
        public String getPackageName() {
            return packageName;
        }
    }
}
