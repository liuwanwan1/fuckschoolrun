package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

public final class FridaInjectionGateway {
    @NonNull
    public ReloadResult reload(@NonNull RootFeatureConfig config) {
        if (!config.isEnabled(RootFeature.FRIDA_DYNAMIC_INJECTION)) {
            return new ReloadResult(false, "Frida动态注入开关关闭。");
        }
        if (config.getInjectionFramework() != RootFeatureConfig.InjectionFramework.FRIDA) {
            return new ReloadResult(false, "仅允许声明Frida框架，当前配置不匹配。");
        }
        return new ReloadResult(
                false,
                "已识别Frida动态注入请求，但项目安全边界禁止 attach/spawn/hook 目标进程；本次热重载只记录配置状态。"
        );
    }

    public static final class ReloadResult {
        private final boolean loaded;
        private final String message;

        private ReloadResult(boolean loaded, @NonNull String message) {
            this.loaded = loaded;
            this.message = message;
        }

        public boolean isLoaded() {
            return loaded;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }
}
