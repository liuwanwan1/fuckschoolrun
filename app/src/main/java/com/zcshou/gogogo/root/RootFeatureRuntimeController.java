package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.EnumMap;

public final class RootFeatureRuntimeController {
    private final FridaInjectionGateway fridaInjectionGateway;

    public RootFeatureRuntimeController() {
        this(new FridaInjectionGateway());
    }

    RootFeatureRuntimeController(@NonNull FridaInjectionGateway fridaInjectionGateway) {
        this.fridaInjectionGateway = fridaInjectionGateway;
    }

    @NonNull
    public RootFeatureRuntimeReport reload(@NonNull RootFeatureConfig config) {
        EnumMap<RootFeature, RootFeatureRuntimeReport.State> states = new EnumMap<>(RootFeature.class);
        EnumMap<RootFeature, String> messages = new EnumMap<>(RootFeature.class);
        for (RootFeature feature : RootFeature.values()) {
            if (!config.isEnabled(feature)) {
                states.put(feature, RootFeatureRuntimeReport.State.DISABLED);
                messages.put(feature, "开关关闭");
                continue;
            }
            if (feature == RootFeature.FRIDA_DYNAMIC_INJECTION) {
                FridaInjectionGateway.ReloadResult result = fridaInjectionGateway.reload(config);
                states.put(feature, result.isLoaded()
                        ? RootFeatureRuntimeReport.State.LOADED
                        : RootFeatureRuntimeReport.State.BLOCKED);
                messages.put(feature, result.getMessage());
                continue;
            }
            if (feature.isRestrictedExecution()) {
                states.put(feature, RootFeatureRuntimeReport.State.BLOCKED);
                messages.put(feature, "按内测安全边界仅记录请求状态，不执行系统注入、信号伪造、Hook修改或检测绕过。");
                continue;
            }
            states.put(feature, RootFeatureRuntimeReport.State.LOADED);
            messages.put(feature, safeFeatureMessage(feature));
        }
        return new RootFeatureRuntimeReport(
                config.getVersion(),
                System.currentTimeMillis(),
                states,
                messages
        );
    }

    @NonNull
    private String safeFeatureMessage(@NonNull RootFeature feature) {
        switch (feature) {
            case ENVIRONMENT_INSPECTION:
                return "Root、Hook、开发者选项和模拟位置状态检测已按开关加载。";
            case ROOT_SHELL_PROBE:
                return "su -c id 授权探测入口已按开关加载。";
            case ENCRYPTED_AUDIT_LOG:
                return "关键操作写入 AES-GCM 加密审计输出。";
            case GM_TEST_INTERFACE:
                return "GM测试接口仅生成内存轨迹、速度和距离数据，不写系统服务。";
            default:
                return "已加载。";
        }
    }
}
