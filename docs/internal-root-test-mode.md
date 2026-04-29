# Root/非Root双模式内测说明

## 入口

进入 `路线模拟` 页面，点击 `模拟设置`，顶部可在 `非Root模式` 与 `Root模式` 间切换。

## 非Root模式

非Root模式保留原有路线模拟设置，继续使用 Android 标准 mock provider 路径完成 GPS、Network provider、NMEA 元数据和路线运动参数模拟。

## Root模式

Root模式仅用于内测环境的状态检测、授权确认与审计记录。当前实现包含：

- Root管理器、su路径、Hook框架包名的环境检测
- 开发者选项、当前应用模拟位置授权、旧版模拟位置全局开关检测
- Root隐藏迹象的启发式提示：检测到Root管理器但未发现常见su路径时提示环境不一致
- 本次测试会话二次确认
- 显式 `su -c id` 授权探测
- 本地审计日志，写入应用私有 SharedPreferences，并同步写入 XLog

## 内测开关

`BuildConfig.INTERNAL_ROOT_TESTING_ENABLED` 控制Root测试操作：

- debug 构建：默认 `true`
- release 构建：默认 `false`

release 构建中Root面板只展示检测结果，确认测试和请求Root按钮不可用。

## 授权流程

1. 切换到 `Root模式`
2. 查看Root状态与设备环境状态
3. 点击 `确认测试`，确认本会话只做检测与审计
4. 点击 `请求Root`，系统如有Root管理器会弹出授权
5. 工具只执行 `su -c id` 探测授权状态，不执行系统注入、绕过或Hook修改

## 明确禁用的能力

以下能力仅以状态说明呈现，当前不会执行：

- Root系统服务NMEA注入
- 基站/WiFi信号伪造
- 模拟位置检测绕过
- 加载Hook模块或修改目标应用返回值
- 读取、篡改系统核心服务数据流
- 注入系统传感器事件

## 算法验证实验室

debug 内测构建会在 Root 模式面板中显示 `算法验证实验室` 入口。该入口启动 `:algorithm_sandbox` 独立调试进程，功能包括：

- 步频算法模拟器：生成加速度、陀螺仪、计步器联动序列，范围限制为 140-220 SPM
- GPS 轨迹生成器：生成符合运动学约束的轨迹点、GPRMC 文本、GPX/KML/CSV/JSON
- 传感器一致性验证：根据步频数据和 GPS 轨迹输出一致性评分报告
- 测试数据管理：仅保存到应用私有 `algorithm-test-cases` 目录

安全边界：

- `algorithm-test-kit` 是纯 Java 库，不依赖 Android 系统 API，不写文件
- app 仅在 `debugImplementation` 引入该库，release 变体不链接算法模块
- debug Activity 位于 `app/src/debug`，release APK 不包含该 Activity
- 所有生成操作需完成：功能开关确认、会话确认、生成确认
- 所有导出操作需额外完成保存确认
- 审计日志写入应用私有 `algorithm_audit.log.enc`，使用 Android Keystore AES-GCM 加密
- 不存在任何系统服务注入、mock provider 写入、Hook 加载或传感器事件注入代码

后续如需扩展Root能力，应保持模块化实现，并继续满足内测开关、用户二次确认、操作审计和最小化权限原则。
