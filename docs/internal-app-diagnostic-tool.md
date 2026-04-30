# 内部应用诊断与加固测试工具

## 使用边界

本工具仅用于公司自有或已获得明确授权的目标 APK。测试必须运行在隔离的 root 测试设备或模拟设置容器中，不得用于第三方应用、生产环境或用户设备。

## 单目标隔离

Root 模式面板新增 `打开LSPosed选择目标APK` 和模块设置入口：

- Root 模式新增总开关；只有 DEBUG 内测构建、设备检测到 Root 迹象并手动开启总开关后，才显示环境、模块、日志等高级控件。未检测到 Root 时总开关保持禁用，避免误触发诊断链路。
- 已接入我司内部项目 [FUCK-RUN](https://github.com/BieFan1029/FUCK-RUN-) 的兼容画像：`com.huachenjie.shandong_school`、`com.huachenjie.shandong_school_pro`、`com.bxkj.student`、`com.zjwh.android_wh_physicalfitness`、`android.youma.com` 均视为公司内部授权测试软件，并写入 LSPosed 推荐作用域；工具不会自动选择作用域，仍由测试人员在 LSPosed 中确认目标。
- 不再把 `org.lsposed.manager` 或 `com.android.shell` 寄生入口检测作为前置条件；LSPosed 现在以框架加载模块为准，目标 APK 进程启动后会由 LSPosed 按作用域加载本模块。界面里的 LSPosed 按钮仅作为可选快捷入口，找不到入口时不阻断诊断广播。
- 本应用按 LSPosed 模块声明：`AndroidManifest.xml` 写入 `xposedmodule`、`xposedminversion=82`、`xposedscope`，`assets/xposed_init` 指向 `RootDiagnosticLsposedModule`。
- 目标 APK 不再由本应用枚举安装列表选择；点击按钮会打开 LSPosed 管理器，由测试人员在本模块的作用域页面勾选目标 APK。
- Hook 入口只在 LSPosed 勾选的作用域进程加载，并排除本应用、Android 系统进程、SystemUI、设置、桌面和安装器。
- 点击路线模拟页的 `开始模拟` 后，如果已确认 Root 测试会话且至少开启一个诊断模块，会向 LSPosed 作用域进程广播诊断开始信号；不再因未检测到管理器 APK 而阻断。
- 暂停路线模拟、路线模拟完成或页面销毁时，会自动结束当前诊断会话并生成报告。

## 模块

当前模块与 Root 能力开关一一对应：

- 定位信号模拟：Hook `LocationManager` 和 `Location`，注入 GPS/NMEA/卫星字段测试数据；路线模拟开始后仍以 `ServiceGo` 原始 test provider/NMEA 注入为主，LSPosed 模块实时接收当前路线帧经纬度和速度作为辅助增强，不要求用户手动填写位置。GPS 强度只允许选择弱/中/强档位，并联动卫星数和 HDOP。
- 基站/Wi-Fi 信号模拟：Hook Wi-Fi 和 Telephony 读取接口，测试环境一致性验证；封闭测试环境画像自动模拟 BSSID、SSID、运营商和国家码，这些字段在 UI 中只读；测试人员只可选择弱/中/强信号档位，Wi-Fi RSSI、Cell dBm 与 GPS 强度同步联动。
- 特定检测绕过测试：Hook root、调试、mock location 等检测接口返回值；可单独开启 root 文件/命令、调试器、mock location 返回值控制。
- 目标应用内部 Hook：在 LSPosed 作用域进程内保留内部检测函数测试计划；可设置最多 Hook 方法数。
- 系统服务数据流控制：控制目标进程内剪贴板、蓝牙、NFC 交互；可单独控制剪贴板、蓝牙、NFC 返回值。
- 传感器数据注入：Hook `SensorManager.registerListener`、`SystemSensorManager`、ColorOS/OnePlus `OplusSensorManager` 和目标进程内 `SensorEventListener`，参考我司内部 [FUCK-RUN](https://github.com/BieFan1029/FUCK-RUN-) 的正弦波模型注入加速度、计步器、步频检测数据；可设置 140-220 SPM 步频范围和 Z 轴波形振幅。目标进程注册计步器/步检测器监听器后，模块会按步频补充合成计步脉冲，避免测试机静止时目标应用只收到 0 步。

## 执行要求

开始测试前必须满足：

- DEBUG 内测构建启用 `BuildConfig.INTERNAL_ROOT_TESTING_ENABLED`
- 应用构建范围为 Android 9 / API 28 到 Android 16 / API 36；Android 13+ 会请求通知权限，Android 14+ 已声明 location 前台服务权限
- Root 模式总开关已开启，且当前设备环境检测到 Root 迹象
- 设备已启用 LSPosed 框架，并已在 LSPosed 作用域中启用本模块和目标 APK；本工具不再检测管理器 APK 是否存在
- 已确认本次 Root 测试会话
- 已在 LSPosed 管理器中启用本模块，并在作用域中勾选目标 APK
- 已开启 LSPosed 作用域注入框架开关和至少一个诊断模块
- 点击 `开始模拟`，诊断自动启动；不再在目标 APK 选择区域提供独立开始/结束按钮
- 如果目标 APK 在路线模拟开始后才启动，目标进程内的 LSPosed 模块会主动向本应用请求当前诊断状态，本应用会重播会话和模块设置；运行中调整步频等模块设置也会同步广播到作用域进程。

Frida 脚本生成与命令注入保留为代码层回退能力；当前 UI 默认使用 LSPosed 作用域模式。

Android 9-16 兼容处理：

- `minSdkVersion` 已对齐 Android 9 / API 28，`compileSdk` / `targetSdkVersion` 已升到 Android 16 / API 36。
- 构建链路升级为 Gradle 8.13、Android Gradle Plugin 8.13.2，并要求本地 SDK 安装 `platforms;android-36` 与 `build-tools;36.1.0`。
- v35 主题临时设置 `android:windowOptOutEdgeToEdgeEnforcement=true`，避免旧页面在 Android 15 强制 edge-to-edge 下被系统栏遮挡；v36 主题单独覆盖并不再依赖该临时属性，后续页面改造应按系统栏 inset 处理。
- `AndroidManifest.xml` 开启 `android:pageSizeCompat="enabled"`，在替换 Baidu 原生库前保留 Android 16 的 16 KB page-size 兼容模式。
- `AndroidManifest.xml` 暂时设置 `android:enableOnBackInvokedCallback="false"`，保护当前仍使用 `onBackPressed()` 的页面行为；后续迁移到 AndroidX `OnBackPressedDispatcher` 后可移除。
- 动态广播接收器在 Android 13+ 使用显式 exported flag；下载完成广播额外校验 download id。
- `ServiceGo` 不再在 Android 12+ 的 task removed 回调里自启动前台服务，避免触发后台前台服务启动限制。
- Debug APK 已可通过 `zipalign -c -P 16 -v 4` 检查 16 KB APK 对齐；当前 Baidu `libindoor.so` 仍是 4 KB ELF LOAD alignment，若未来要完整覆盖 16 KB page-size 设备，需要替换为 16 KB ELF 对齐版本。

## 输出

结束测试时生成：

- `files/root-diagnostic/<session>.report.md`
- `files/root-diagnostic/<session>.report.json`

报告包含检测机制有效性分析、传感器健壮性评估、时间序列事件日志和代码级修复建议。关键操作继续写入 AES-GCM 加密审计日志。

Root 设置底部新增 `日志输出` 模块：诊断运行期间，LSPosed 作用域进程会采集目标进程内的 `android.util.Log`、`System.out/err` 和 `Throwable.printStackTrace`，按日期写入 `files/root-diagnostic/process-logs/<yyyy-MM-dd>.log`。页面按日期分类展示日志，单日详情从新到旧排序，并提供一键复制给开发人员排查兼容性问题。
