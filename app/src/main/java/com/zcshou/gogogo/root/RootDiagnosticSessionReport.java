package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RootDiagnosticSessionReport {
    private final String sessionId;
    private final String targetPackageName;
    private final long startedAtMillis;
    private final long endedAtMillis;
    private final List<RootDiagnosticModule> modules;
    private final List<RootDiagnosticEvent> events;
    private final String scriptPath;
    private final String manualAttachCommand;
    private final String manualSpawnCommand;

    RootDiagnosticSessionReport(
            @NonNull String sessionId,
            @NonNull String targetPackageName,
            long startedAtMillis,
            long endedAtMillis,
            @NonNull List<RootDiagnosticModule> modules,
            @NonNull List<RootDiagnosticEvent> events,
            @NonNull String scriptPath,
            @NonNull String manualAttachCommand,
            @NonNull String manualSpawnCommand
    ) {
        this.sessionId = sessionId;
        this.targetPackageName = targetPackageName;
        this.startedAtMillis = startedAtMillis;
        this.endedAtMillis = endedAtMillis;
        this.modules = Collections.unmodifiableList(new ArrayList<>(modules));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.scriptPath = scriptPath;
        this.manualAttachCommand = manualAttachCommand;
        this.manualSpawnCommand = manualSpawnCommand;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    @NonNull
    public String getTargetPackageName() {
        return targetPackageName;
    }

    @NonNull
    public List<RootDiagnosticModule> getModules() {
        return modules;
    }

    @NonNull
    public List<RootDiagnosticEvent> getEvents() {
        return events;
    }

    @NonNull
    public String toText() {
        StringBuilder builder = new StringBuilder(16 * 1024);
        builder.append("# 内部应用诊断报告\n\n");
        builder.append("目标APK：").append(targetPackageName).append('\n');
        builder.append("会话ID：").append(sessionId).append('\n');
        builder.append("开始时间：").append(formatTime(startedAtMillis)).append('\n');
        builder.append("结束时间：").append(formatTime(endedAtMillis)).append('\n');
        builder.append("Frida脚本：").append(scriptPath).append('\n');
        builder.append("手动attach命令：").append(manualAttachCommand).append('\n');
        builder.append("进程启动注入命令：").append(manualSpawnCommand).append("\n\n");

        builder.append("## 启用模块\n");
        for (RootDiagnosticModule module : modules) {
            builder.append("- ").append(module.getTitle())
                    .append("：").append(module.getHookSurface())
                    .append("；").append(module.getTestPurpose())
                    .append('\n');
        }
        if (modules.isEmpty()) {
            builder.append("- 未启用诊断模块。\n");
        }

        builder.append("\n## 检测机制有效性分析\n");
        for (RootDiagnosticModule module : modules) {
            int hookEvents = countEvents(module, "hook_installed", "scan_complete", "api_call");
            int bypassEvents = countEvents(module, "return_override", "value_override", "data_blocked");
            builder.append("- ").append(module.getTitle()).append("：");
            if (hookEvents == 0 && bypassEvents == 0) {
                builder.append("本次未捕获到目标进程调用，需在目标APK启动后重跑或使用进程启动注入命令。");
            } else {
                builder.append("捕获关键事件 ").append(hookEvents + bypassEvents).append(" 条");
                if (bypassEvents > 0) {
                    builder.append("，其中返回值/数据流被受控改写 ").append(bypassEvents).append(" 次");
                }
                builder.append("。");
            }
            builder.append('\n');
        }

        builder.append("\n## 传感器代码健壮性评估\n");
        RootDiagnosticModule sensorModule = RootDiagnosticModule.SENSOR_INJECTION;
        if (modules.contains(sensorModule)) {
            int injected = countEvents(sensorModule, "data_injected", "inject_error");
            if (injected > 0) {
                builder.append("已向目标进程内传感器监听器注入突变样本；请重点检查UI卡顿、主线程计算、数组边界和异常值处理日志。\n");
            } else {
                builder.append("未捕获到目标进程传感器监听器调用；建议启动目标APK的运动/跑步页面后再次测试。\n");
            }
        } else {
            builder.append("本次未启用传感器数据注入模块。\n");
        }

        builder.append("\n## 详细问题日志\n");
        if (events.isEmpty()) {
            builder.append("无事件。\n");
        } else {
            for (RootDiagnosticEvent event : events) {
                builder.append("- ").append(event.toDisplayLine()).append('\n');
            }
        }

        builder.append("\n## 具体修复建议\n");
        for (RootDiagnosticModule module : modules) {
            builder.append("- ").append(module.getTitle()).append("：").append(module.getRemediation()).append('\n');
        }
        builder.append("- 通用加固：关键检测结果不要只依赖客户端单点布尔值，建议结合服务端风控、时间序列一致性、异常路径日志和灰度降级策略。\n");
        builder.append("- 合规边界：本报告仅用于公司自有或已授权应用的内部测试，不得用于第三方应用或生产用户设备。\n");
        return builder.toString();
    }

    @NonNull
    public String toJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("sessionId", sessionId);
            root.put("targetPackageName", targetPackageName);
            root.put("startedAtMillis", startedAtMillis);
            root.put("endedAtMillis", endedAtMillis);
            root.put("scriptPath", scriptPath);
            root.put("manualAttachCommand", manualAttachCommand);
            root.put("manualSpawnCommand", manualSpawnCommand);
            JSONArray moduleArray = new JSONArray();
            for (RootDiagnosticModule module : modules) {
                JSONObject item = new JSONObject();
                item.put("id", module.getId());
                item.put("title", module.getTitle());
                item.put("hookSurface", module.getHookSurface());
                item.put("testPurpose", module.getTestPurpose());
                item.put("remediation", module.getRemediation());
                moduleArray.put(item);
            }
            root.put("modules", moduleArray);
            JSONArray eventArray = new JSONArray();
            for (RootDiagnosticEvent event : events) {
                eventArray.put(event.toJsonObject());
            }
            root.put("events", eventArray);
            root.put("textReport", toText());
        } catch (Exception ignored) {
            // Keep best-effort JSON output.
        }
        return root.toString();
    }

    private int countEvents(@NonNull RootDiagnosticModule module, @NonNull String... tokens) {
        int count = 0;
        for (RootDiagnosticEvent event : events) {
            if (!event.belongsToModule(module)) {
                continue;
            }
            for (String token : tokens) {
                if (event.containsSignal(token)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    @NonNull
    private String formatTime(long timestampMillis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(timestampMillis));
    }
}
