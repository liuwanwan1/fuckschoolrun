## LSPatch 免Root模块 v1.0.0

适配**支付宝阳光校园跑**的免Root步频+GPS模拟模块。

### 功能

| 能力 | 实现 |
|------|------|
| 步频传感器注入 | 加速度计正弦波 + 计步器线性增长 + 步检测器脉冲 |
| 主动脉冲注入 | 每100ms反射构造SensorEvent并调用onSensorChanged |
| 设备无传感器降级 | getDefaultSensor返回加速度计兜底 |
| Mock GPS隐藏 | isFromMockProvider→false, mock_location→0 |
| 配置热更新 | 每5秒读取 /sdcard/schoolrun_lspatch.json |

### 使用步骤

1. 下载 `SchoolRun-lspatch-v1.0.0.apk`
2. 在 [LSPatch Manager](https://github.com/LSPosed/LSPatch) 中嵌入到支付宝APK
3. 安装修补后的支付宝
4. 配合 [SchoolRun 主App](https://github.com/liuwanwan1/fuckschoolrun) 使用

### 默认参数

```json
{
  "cadence_spm": 180,
  "min_cadence": 140,
  "max_cadence": 220,
  "wave_amplitude": 2.5,
  "step_length_m": 0.8
}
```

### 兼容性

- Android 9.0+ (API 28+)
- 支付宝 (com.eg.android.AlipayGphone)
- 无需 Root / Magisk / LSPosed

> 🤖 Generated with [Claude Code](https://claude.com/claude-code)
