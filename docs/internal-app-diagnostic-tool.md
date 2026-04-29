# 内部应用诊断与加固测试工具

## 使用边界

本工具仅用于公司自有或已获得明确授权的目标 APK。测试必须运行在隔离的 root 测试设备或模拟设置容器中，不得用于第三方应用、生产环境或用户设备。

## 单目标隔离

Root 模式面板新增 `选择目标APK` 和模块设置入口：

- 未选择目标 APK 时，诊断 Hook 计划保持禁用。
- 目标 APK 选择弹窗支持按应用名、包名、版本搜索。
- 目标包名写入应用私有 `root_feature_config.json`。
- 生成的 Frida 脚本内置 `TARGET_PACKAGE` 运行时校验，当前进程包名不等于目标包名时立即退出，不安装任何 Hook。
- 自动启动仅尝试 `/data/local/tmp/frida-inject` 或 `/data/local/tmp/frida`，命令参数限定到目标包名。
- 点击路线模拟页的 `开始模拟` 后，如果已选择目标 APK、已确认 Root 会话且授权通过，会自动启动目标 APK 诊断。
- 暂停路线模拟、路线模拟完成或页面销毁时，会自动结束当前诊断会话并生成报告。

## 模块

当前模块与 Root 能力开关一一对应：

- 定位信号模拟：Hook `LocationManager` 和 `Location`，注入 GPS/NMEA/卫星字段测试数据；可设置经纬度、速度、卫星数和 HDOP。
- 基站/Wi-Fi 信号模拟：Hook Wi-Fi 和 Telephony 读取接口，测试环境一致性验证；可设置 BSSID、SSID、运营商和国家码。
- 特定检测绕过测试：Hook root、调试、mock location 等检测接口返回值；可单独开启 root 文件/命令、调试器、mock location 返回值控制。
- 目标应用内部 Hook：只枚举目标包命名空间内疑似检测类和布尔检测方法；可设置最多 Hook 方法数。
- 系统服务数据流控制：控制目标进程内剪贴板、蓝牙、NFC 交互；可单独控制剪贴板、蓝牙、NFC 返回值。
- 传感器数据注入：Hook `SensorManager.registerListener` 和目标进程内 `SensorEventListener`，参考 [FUCK-RUN](https://github.com/BieFan1029/FUCK-RUN-) 的正弦波模型注入加速度、计步器、步频检测数据；可设置 140-220 SPM 步频范围和 Z 轴波形振幅。

## 执行要求

开始测试前必须满足：

- DEBUG 内测构建启用 `BuildConfig.INTERNAL_ROOT_TESTING_ENABLED`
- 已确认本次 Root 测试会话
- `su -c id` 授权探测通过
- 已选择目标 APK
- 已开启 Frida 框架开关和至少一个诊断模块
- 点击 `开始模拟`，诊断自动启动；不再在目标 APK 选择区域提供独立开始/结束按钮

若设备未安装 Frida 命令，工具仍会生成脚本和报告中的手动命令：

```bash
frida -U com.example.target -l /data/data/com.acooldog.toolbox/files/root-diagnostic/<session>.frida.js
frida -U -f com.example.target -l /data/data/com.acooldog.toolbox/files/root-diagnostic/<session>.frida.js
```

## 输出

结束测试时生成：

- `files/root-diagnostic/<session>.frida.js`
- `files/root-diagnostic/<session>.report.md`
- `files/root-diagnostic/<session>.report.json`

报告包含检测机制有效性分析、传感器健壮性评估、时间序列事件日志和代码级修复建议。关键操作继续写入 AES-GCM 加密审计日志。
