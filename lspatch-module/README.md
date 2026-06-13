# SchoolRun LSPatch Module — 免Root步频模拟

LSPatch 免 Root Xposed 模块，向目标 App 的传感器监听器注入模拟加速度计和计步器数据。

专为 **支付宝阳光校园跑** 适配，配合 LSPatch Manager 使用即可在非 Root 设备上实现完整的 GPS + 步频传感器一致性模拟。

## 工作原理

```
┌─────────────────────────────────────────────┐
│ 支付宝 (LSPatch修补后)                        │
│                                              │
│  阳光校园跑 → SensorManager.registerListener │
│       ↓                                      │
│  LspatchStepModule Hook 拦截                 │
│       ↓                                      │
│  onSensorChanged() 返回伪造数据:              │
│  • 加速度计: 正弦波形 (9.81 + 振幅×sin(ωt))  │
│  • 计步器: 线性递增 (base + t × cadence/60)  │
│  • 步检测器: 脉冲 1.0                        │
└─────────────────────────────────────────────┘
         +
┌─────────────────────────────────────────────┐
│ SchoolRun App (本项目的完整APK)               │
│                                              │
│  GPS Mock Location → 模拟位置                 │
│  CADENCE模式 → 步频换算速度                   │
│  Real-Run Link → 真实步数联动                 │
└─────────────────────────────────────────────┘
```

## 前置条件

1. 安装 [LSPatch](https://github.com/LSPosed/LSPatch) Manager APK
2. 备份支付宝数据（修补过程需要卸载原版支付宝）
3. Android 9.0+（已测试 Android 9-15）

## 使用步骤

### 1. 构建模块

```bash
./gradlew :lspatch-module:assembleRelease
```

产出文件: `lspatch-module/build/outputs/apk/release/lspatch-module-release.apk`

### 2. LSPatch 修补支付宝

1. 打开 LSPatch Manager
2. 点击「管理」→ 选择「支付宝」的 APK
3. 选择「嵌入模块」模式
4. 勾选 `lspatch-module-release.apk`
5. 开始修补
6. 安装修补后的支付宝 APK（需先卸载原版）

### 3. （可选）配置步频参数

在 `/sdcard/schoolrun_lspatch.json` 写入自定义配置：

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
| `wave_amplitude` | 加速度Z轴波形振幅 | 2.5 | 0.5-10.0 |
| `step_length_m` | 步长（米） | 0.8 | 0.3-2.0 |

配置热更新：模块每 5 秒自动重新读取配置文件，无需重启支付宝。

### 4. 配合主 App 使用

1. 启动 SchoolRun 主 App
2. 选择路线，设置 **步频模式 (CADENCE)**
3. 步频值与 LSPatch 模块的 `cadence_spm` 保持一致
4. 开启 **真实跑步联动 (Real-Run Link)**，由真实步数驱动定位
5. 或直接点击「开始」使用 GPS Mock Location 模拟位置

## 验证方式

### 通过 logcat 验证

```bash
adb logcat | grep "SchoolRunLSP"
```

预期输出：
```
[SchoolRunLSP] Loaded into Alipay process: com.eg.android.AlipayGphone
[SchoolRunLSP] Module activated. cadence=180 SPM range=140-220
[SchoolRunLSP] Captured sensor listener: com.alipay.xxx.SensorListener for sensor type=1
[SchoolRunLSP] accelerometer cadence=180 z=10.52
[SchoolRunLSP] pulse: cadence=180 listeners=3 elapsed=10s
```

### 在支付宝内验证

1. 打开支付宝 → 进入阳光校园跑
2. 开始跑步或自由跑
3. 观察步数和 GPS 轨迹是否与模拟参数一致

## 依赖

- 编译期: Xposed API 82 (compileOnly)
- 运行时: LSPatch 框架 (由 LSPatch Manager 提供)

## 注意事项

- 本模块仅在支付宝主进程生效
- 不同手机的传感器行为有差异，如遇异常可调整 `wave_amplitude` 参数
- LSPatch 修补后的支付宝可能触发 Google Play Protect 警告，属正常现象
- 如阳光校园跑有签名校验，需配合签名伪装或其他绕过方案

## 与完整 Root 方案的对比

| 能力 | LSPatch 方案 | Root LSPosed 方案 |
|------|-------------|-------------------|
| 需要 Root | ❌ 不需要 | ✅ 需要 Magisk + LSPosed |
| 需要解锁 Bootloader | ❌ 不需要 | ✅ 需要 |
| 传感器数据注入 | ✅ | ✅ |
| GPS Mock Location | 配合主App | ✅ 内置 |
| 环境隐藏 | ❌ | ✅ |
| NMEA 注入 | ❌ | ✅ |
| 信号强度模拟 | ❌ | ✅ |
