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

后续如需扩展Root能力，应保持模块化实现，并继续满足内测开关、用户二次确认、操作审计和最小化权限原则。
