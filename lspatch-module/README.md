# SchoolRun LSPatch Module — 免Root步频+GPS模拟

基于上游 `RootDiagnosticLsposedModule` 打包的 LSPatch 免 Root Xposed 模块，
向支付宝阳光校园跑注入传感器数据、模拟 GPS 位置、伪造 WiFi/基站信号并绕过检测。

## 工作原理

```
┌─────────────────────────────────────────────────────────┐
│ 支付宝 (LSPatch 修补后)                                  │
│                                                          │
│  ── 自动激活 (3s内无广播即自启) ──                       │
│                                                          │
│  SENSOR_INJECTION:                                       │
│    脉冲循环(100ms) → 反射创建 SensorEvent                │
│    → listener.onSensorChanged()                          │
│    加速度计正弦波 + 计步器线性增长 + 步检测器脉冲        │
│                                                          │
│  LOCATION_NMEA:                                          │
│    Location.isFromMockProvider() → false                 │
│    Location.getLatitude() / getLongitude() 注入          │
│    GMS / 高德 / 百度 / 腾讯 SDK 全面覆盖                │
│    NMEA listener + GnssStatus 注入                       │
│                                                          │
│  RADIO_WIFI_SIGNAL:                                      │
│    WiFi BSSID/SSID/RSSI 模拟                             │
│    基站 CellSignalStrength 模拟                          │
│    TelephonyManager 网络信息伪装                          │
│                                                          │
│  DETECTION_BYPASS:                                       │
│    Debug.isDebuggerConnected() → false                   │
│    Settings.Secure.mock_location → 0                     │
│    File.exists(/su, /magisk, xposed, frida) → false     │
└─────────────────────────────────────────────────────────┘
         +
┌─────────────────────────────────────────────────────────┐
│ SchoolRun 主App                                          │
│                                                          │
│  GPS Mock Location → 模拟位置                             │
│  CADENCE 模式 → 步频换算速度                              │
│  Real-Run Link → 真实步数联动 (可选)                      │
│  Root 诊断面板 → 远程控制模块 (可选)                      │
└─────────────────────────────────────────────────────────┘
```

## 前置条件

1. 安装 [LSPatch Manager](https://github.com/LSPosed/LSPatch)
2. 备份支付宝数据（修补前需卸载原版）
3. Android 9.0+（已测试 Android 9-15）

## 使用步骤

### 1. 构建模块

```bash
./gradlew :lspatch-module:assembleRelease
```

输出: `lspatch-module/build/outputs/apk/release/lspatch-module-release.apk`

### 2. LSPatch 修补支付宝

1. 打开 LSPatch Manager
2. 点击「管理」→ 选择支付宝 APK
3. 选择「嵌入模块」模式
4. 勾选 `lspatch-module-release.apk`
5. 开始修补
6. 安装修补后的支付宝 APK（需先卸载原版）

### 3. 配合主 App 使用

1. 启动 SchoolRun 主 App
2. 选择路线，设置 **步频模式 (CADENCE)**
3. 步频值默认 180 SPM
4. 点击「开始」运行 GPS 模拟
5. 切换支付宝 → 阳光校园跑 → 开始跑步

模块会自动激活，无需额外配置。

## 验证

### logcat 验证

```bash
adb logcat | grep "SchoolRunDiag"
```

预期输出：
```
SchoolRunDiag LSPatch auto-started for: com.eg.android.AlipayGphone modules=4
SchoolRunDiag LSPosed loaded for package: com.eg.android.AlipayGphone
```

### 功能验证

1. 打开修补后的支付宝 → 阳光校园跑
2. 观察步数是否持续增长
3. GPS 轨迹应正常
4. 无 Mock Location 检测提示

## 技术架构

本模块通过 `xposed-core` Android Library 共享上游项目的成熟 Root 诊断框架:

```
xposed-core/                    ← 共享库
  └─ RootDiagnosticLsposedModule (IXposedHookLoadPackage)
  └─ LsposedDiagnosticBridge (Broadcast 通信)
  └─ RootDiagnosticSettings (JSON 配置)
  └─ ... (共 9 个核心类)
      ↑                   ↑
app 依赖            lspatch-module 依赖
(LSPosed 模式)      (LSPatch 模式)
```

## 与完整 Root 方案的对比

| 能力 | LSPatch 方案 | Root LSPosed 方案 |
|------|-------------|-------------------|
| 需要 Root | ❌ 不需要 | ✅ Magisk + LSPosed |
| 解锁 Bootloader | ❌ 不需要 | ✅ 需要 |
| 传感器数据注入 | ✅ | ✅ |
| GPS/NMEA 注入 | ✅ | ✅ |
| Mock Location 隐藏 | ✅ | ✅ |
| WiFi/基站信号模拟 | ✅ | ✅ |
| 检测绕过 | ✅ | ✅ |
| 远程控制面板 | 通过 Broadcast | ✅ |
