package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum RootDiagnosticModule {
    LOCATION_NMEA(
            RootFeature.ROOT_NMEA_INJECTION,
            "定位信号模拟",
            "LocationManager / Location / NMEA",
            "注入带卫星数、HDOP、速度和NMEA摘要的定位响应，验证位置防伪逻辑。",
            "建议校验Location时间连续性、elapsedRealtimeNanos、satellites/hdop扩展字段和NMEA时间戳一致性。"
    ),
    RADIO_WIFI_SIGNAL(
            RootFeature.SIGNAL_SIMULATION,
            "基站/Wi-Fi信号模拟",
            "WifiManager / TelephonyManager",
            "控制Wi-Fi BSSID、SSID、基站ID和网络归属信息，验证环境一致性检测。",
            "建议将GPS、基站、Wi-Fi、IP属地和历史轨迹做一致性评分，不依赖单一环境信号。"
    ),
    DETECTION_BYPASS(
            RootFeature.MOCK_LOCATION_BYPASS,
            "特定检测绕过测试",
            "File / Runtime / Debug / Settings",
            "临时改写root、调试、mock location等检测接口返回值，观察目标应用降级策略。",
            "建议关键检测结果服务端二次校验，并对检测函数被异常短路、返回值恒定等情况建立风控分支。"
    ),
    TARGET_APP_HOOK(
            RootFeature.TARGET_APP_HOOK,
            "目标应用内部Hook",
            "目标包内疑似check/is/detect方法",
            "仅枚举目标包命名空间内疑似检测类和布尔检测方法，验证检测函数被Hook后的业务行为。",
            "建议对关键检测函数做调用链完整性校验、结果交叉验证和异常路径熔断，避免单点布尔返回决定安全状态。"
    ),
    SERVICE_STREAM(
            RootFeature.SYSTEM_SERVICE_STREAM_LOG,
            "系统服务数据流控制",
            "Clipboard / Bluetooth / NFC",
            "控制剪贴板、蓝牙、NFC等系统服务返回值和数据流，测试异常服务交互处理。",
            "建议对系统服务返回null、权限异常、状态瞬变和空数据做显式兜底，避免阻塞主线程或崩溃。"
    ),
    SENSOR_INJECTION(
            RootFeature.SENSOR_EVENT_INJECTION,
            "传感器数据注入",
            "SensorManager / SensorEventListener",
            "向目标进程内的传感器监听器注入突变加速度和陀螺仪样本，验证传感器处理健壮性。",
            "建议在onSensorChanged前增加低通滤波、突变阈值、物理合理性范围和采样时间间隔校验。"
    );

    private final RootFeature feature;
    private final String title;
    private final String hookSurface;
    private final String testPurpose;
    private final String remediation;

    RootDiagnosticModule(
            @NonNull RootFeature feature,
            @NonNull String title,
            @NonNull String hookSurface,
            @NonNull String testPurpose,
            @NonNull String remediation
    ) {
        this.feature = feature;
        this.title = title;
        this.hookSurface = hookSurface;
        this.testPurpose = testPurpose;
        this.remediation = remediation;
    }

    @NonNull
    public RootFeature getFeature() {
        return feature;
    }

    @NonNull
    public String getId() {
        return feature.getConfigKey();
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getHookSurface() {
        return hookSurface;
    }

    @NonNull
    public String getTestPurpose() {
        return testPurpose;
    }

    @NonNull
    public String getRemediation() {
        return remediation;
    }

    public boolean isEnabled(@NonNull RootFeatureConfig config) {
        return config.isEnabled(feature);
    }

    @NonNull
    public static List<RootDiagnosticModule> enabledIn(@NonNull RootFeatureConfig config) {
        List<RootDiagnosticModule> modules = new ArrayList<>();
        for (RootDiagnosticModule module : values()) {
            if (module.isEnabled(config)) {
                modules.add(module);
            }
        }
        return Collections.unmodifiableList(modules);
    }

    @NonNull
    public static String summarizeEnabled(@NonNull RootFeatureConfig config) {
        List<RootDiagnosticModule> modules = enabledIn(config);
        if (modules.isEmpty()) {
            return "未开启诊断模块";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < modules.size(); index++) {
            if (index > 0) {
                builder.append("、");
            }
            builder.append(modules.get(index).getTitle());
        }
        return builder.toString();
    }
}
