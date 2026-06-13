# SchoolRun LSPatch Module — 免Root步频+GPS模拟

LSPatch 免 Root Xposed 模块，向目标 App（支付宝阳光校园跑）注入模拟加速度计、计步器、步检测器数据，同时隐藏 GPS Mock Location 痕迹。

## 工作原理

```
┌─────────────────────────────────────────────────────────┐
│ 支付宝 (LSPatch 修补后)                                  │
│                                                          │
│  ── 传感器注入 ──                                        │
│  脉冲循环 (100ms, Handler mainLooper)                    │
│       │                                                  │
│       ├─ 反射创建 SensorEvent(int valueSize)             │
│       ├─ 填充传感器数据:                                 │
│       │   • 加速度计: 正弦波 (9.81 ± 振幅 × sin(ωt))   │
│       │   • 计步器:   线性增长 (base + t × 步频/60)    │
│       │   • 步检测器: 脉冲 1.0                          │
│       └─ listener.onSensorChanged(event) ← 主动调用!    │
│                                                          │
│  ── Mock Location 隐藏 ──                                │
│  Location.isFromMockProvider() → false                   │
│  Location.isMock() → false                               │
│  Settings.Secure.getString("mock_location") → "0"        │
│  Settings.Secure.getInt("mock_location") → 0             │
└─────────────────────────────────────────────────────────┘
         +
┌─────────────────────────────────────────────────────────┐
│ SchoolRun 主App                                          │
│                                                          │
│  GPS Mock Location → 模拟位置 (addTestProvider)          │
│  CADENCE 模式 → 步频换算速度                              │
│  Real-Run Link → 真实步数联动 (可选)                      │
└─────────────────────────────────────────────────────────┘
```

### 核心设计决策

| 设计 | 说明 |
|------|------|
| **主动脉冲注入** | 不依赖系统传感器回调。每 100ms 通过反射构造 SensorEvent 并主动调用 `listener.onSensorChanged()`，确保无物理传感器的设备也能收到数据 |
| **双重保障** | onSensorChanged Hook + 脉冲循环同时工作。系统回调触发时 Hook 篡改数据；脉冲循环主动注入确保持续数据流 |
| **传感器降级** | 设备缺少计步器时 getDefaultSensor 返回加速度计，标记降级后自动修正目标类型映射 |
| **Mock Location 隐藏** | Hook `Location.isFromMockProvider()` / `Location.isMock()` / `Settings.Secure`，让 SchoolRun 主App 注入的 GPS 位置对校园跑看起来像真实 GPS |
| **热更新** | 每 5 秒重读 `/sdcard/schoolrun_lspatch.json`，无需重启支付宝 |

## 前置条件

1. 安装 [LSPatch Manager](https://github.com/LSPosed/LSPatch)
2. 备份支付宝数据（修补前需卸载原版）
3. Android 9.0+（已测试 Android 9-15）

## 使用步骤

### 1. 构建模块

```bash
./gradlew :lspatch-module:assembleRelease
```

输出: `lspatch-module/build/outputs/apk/release/lspatch-module-release-unsigned.apk`

### 2. LSPatch 修补支付宝

1. 打开 LSPatch Manager
2. 点击「管理」→ 选择支付宝 APK
3. 选择「嵌入模块」模式
4. 勾选 `lspatch-module-release-unsigned.apk`
5. 开始修补
6. 安装修补后的支付宝 APK（需先卸载原版）

### 3. （可选）配置步频参数

在 `/sdcard/schoolrun_lspatch.json` 写入：

```json
{
  "cadence_spm": 180,
  "min_cadence": 140,
  "max_cadence": 220,
  "wave_amplitude": 2.5,
  "step_length_m": 0.8
}
```

| 参数 | 说明 | 默认值 | 范围 |
|------|------|--------|------|
| `cadence_spm` | 目标步频 SPM | 180 | 60-300 |
| `min_cadence` | 随机步频下限 | 140 | 60-300 |
| `max_cadence` | 随机步频上限 | 220 | 60-300 |
| `wave_amplitude` | 加速度计 Z 轴波形振幅 (m/s²) | 2.5 | 0.5-10.0 |
| `step_length_m` | 步长 (米) | 0.8 | 0.3-2.0 |

配置热更新：模块每 5 秒自动重新读取，无需重启支付宝。

### 4. 配合主 App 使用

1. 启动 SchoolRun 主 App
2. 选择路线，设置 **步频模式 (CADENCE)**
3. 步频值建议与 LSPatch 模块的 `cadence_spm` 一致（默认 180 SPM）
4. 可选：开启 **真实跑步联动**，由手机真实步数驱动定位速度
5. 点击「开始」运行 GPS Mock Location

## 验证

### logcat 验证

```bash
adb logcat | grep "SchoolRunLSP"
```

预期输出：

```
[SchoolRunLSP] Loaded into: com.eg.android.AlipayGphone
[SchoolRunLSP] Cached sensors: accel=true stepCounter=true stepDetector=true
[SchoolRunLSP] Activated. cadence=180 SPM range=140-220
[SchoolRunLSP] SensorManager hooks installed.
[SchoolRunLSP] Sensor availability hook installed.
[SchoolRunLSP] Location.isFromMockProvider() → false
[SchoolRunLSP] Settings.Secure.getString → mock_location=0
[SchoolRunLSP] Mock location hiding hooks installed.
[SchoolRunLSP] Captured: com.alipay.xxx.StepListener targetType=19
[SchoolRunLSP] pulse: cadence=180 listeners=3 elapsed=10s sensors=3
[SchoolRunLSP] pulse: cadence=180 listeners=3 elapsed=20s sensors=3
```

### 功能验证

1. 打开修补后的支付宝 → 进入阳光校园跑
2. 开始跑步 / 自由跑
3. 观察步数是否持续增长（应与模拟步频一致）
4. 配合 SchoolRun 主App 的 GPS Mock Location → GPS 轨迹与步频数据一致 → 通过反作弊校验

## 依赖

- 编译期: Xposed API 82 (compileOnly)
- 运行时: LSPatch 框架（由 LSPatch Manager 在修补时嵌入）

## 注意事项

- 本模块在支付宝主进程和子进程均生效
- 不同 ROM 的传感器行为有差异，如步数增长异常可调整 `wave_amplitude`
- LSPatch 修补的支付宝可能触发 Google Play Protect 警告，属正常现象
- 支付宝可能检测 Xposed/LSPatch 环境，如遇闪退需配合反检测方案

## 与完整 Root 方案的对比

| 能力 | LSPatch 方案 | Root LSPosed 方案 |
|------|-------------|-------------------|
| 需要 Root | ❌ 不需要 | ✅ Magisk + LSPosed |
| 解锁 Bootloader | ❌ 不需要 | ✅ 需要 |
| 传感器数据注入 | ✅ 主动脉冲 + 反射 | ✅ 系统级 Hook |
| 无物理传感器支持 | ✅ 反射构造 SensorEvent | ✅ Hook 系统 API |
| GPS Mock Location | 配合主App | ✅ 内置 |
| 环境隐藏 | ❌ | ✅ |
| NMEA 注入 | ❌ | ✅ |
| 信号强度模拟 | ❌ | ✅ |
