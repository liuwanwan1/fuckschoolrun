## SchoolRun LSPatch Module v2.0.0

基于上游 `RootDiagnosticLsposedModule` 的完整免Root方案。

### v2.0.0 更新

| 新增能力 | 说明 |
|---------|------|
| GPS/NMEA 注入 | Location / GMS / 高德 / 百度 / 腾讯 SDK 全覆盖 |
| WiFi/基站信号模拟 | BSSID / RSSI / CellSignalStrength / TelephonyManager |
| 检测绕过 | Debugger / Root / Magisk / Xposed / mock_location |
| 自动激活 | 3秒无LSPosed广播即自启4个模块 |
| 架构重构 | 通过 xposed-core 共享库与主App共享代码 |

### 完整功能

- SENSOR_INJECTION — 加速度计正弦波 + 计步器线性增长 + 步检测器脉冲
- LOCATION_NMEA — GPS位置/NMEA注入 + Mock Location隐藏
- RADIO_WIFI_SIGNAL — WiFi/基站信号模拟
- DETECTION_BYPASS — 检测绕过

### 使用方式

1. 下载 `SchoolRun-lspatch-v2.0.0.apk`
2. LSPatch Manager → 嵌入模块 → 选择支付宝 APK
3. 安装修补后的支付宝
4. 配合 SchoolRun 主App 开始跑步
