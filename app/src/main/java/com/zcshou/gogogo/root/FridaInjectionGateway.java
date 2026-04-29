package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class FridaInjectionGateway {
    private static final String[] FRIDA_INJECT_PATHS = new String[] {
            "/data/local/tmp/frida-inject",
            "/system/bin/frida-inject",
            "/system/xbin/frida-inject"
    };
    private static final String[] FRIDA_CLI_PATHS = new String[] {
            "/data/local/tmp/frida",
            "/system/bin/frida",
            "/system/xbin/frida"
    };

    private java.lang.Process activeProcess;
    private Thread stdoutThread;
    private Thread stderrThread;

    @NonNull
    public ReloadResult reload(@NonNull RootFeatureConfig config) {
        if (!config.isEnabled(RootFeature.FRIDA_DYNAMIC_INJECTION)) {
            return new ReloadResult(false, "Frida动态注入开关关闭。");
        }
        if (config.getInjectionFramework() != RootFeatureConfig.InjectionFramework.FRIDA) {
            return new ReloadResult(false, "仅允许声明Frida框架，当前配置不匹配。");
        }
        if (config.getTargetPackageName().trim().isEmpty()) {
            return new ReloadResult(false, "请先选择目标APK；未绑定目标包名时不会生成或启动Hook。");
        }
        return new ReloadResult(
                true,
                "Frida单目标诊断框架已准备；Hook脚本会在目标进程内校验包名后才安装。"
        );
    }

    @NonNull
    public synchronized StartResult startSession(
            @NonNull RootFeatureConfig config,
            @NonNull File scriptFile,
            @NonNull EventSink eventSink
    ) {
        if (activeProcess != null) {
            return new StartResult(false, false, "", "已有Frida诊断进程在运行。");
        }
        String targetPackageName = config.getTargetPackageName().trim();
        if (targetPackageName.isEmpty()) {
            return new StartResult(false, false, "", "未选择目标APK。");
        }
        if (!isSafePackageName(targetPackageName)) {
            return new StartResult(false, false, "", "目标包名包含非法字符，已拒绝启动。");
        }
        String command = buildCommand(targetPackageName, scriptFile.getAbsolutePath());
        if (command.isEmpty()) {
            return new StartResult(
                    true,
                    false,
                    "",
                    "已生成Frida脚本，但测试设备未发现 /data/local/tmp/frida-inject 或 frida 命令；请使用报告中的手动命令注入。"
            );
        }
        try {
            activeProcess = Runtime.getRuntime().exec(new String[] {"su", "-c", command});
            stdoutThread = startPump("stdout", activeProcess.getInputStream(), eventSink);
            stderrThread = startPump("stderr", activeProcess.getErrorStream(), eventSink);
            return new StartResult(true, true, command, "Frida命令已通过su启动，限定目标：" + targetPackageName);
        } catch (Exception exception) {
            activeProcess = null;
            return new StartResult(
                    true,
                    false,
                    command,
                    "Frida命令启动失败：" + exception.getClass().getSimpleName() + ": " + exception.getMessage()
            );
        }
    }

    @NonNull
    public synchronized String stopSession() {
        if (activeProcess == null) {
            return "未发现活动Frida诊断进程。";
        }
        try {
            activeProcess.destroy();
            return "已停止本工具启动的Frida诊断进程。";
        } catch (Exception exception) {
            return "停止Frida诊断进程失败：" + exception.getClass().getSimpleName();
        } finally {
            activeProcess = null;
            if (stdoutThread != null) {
                stdoutThread.interrupt();
                stdoutThread = null;
            }
            if (stderrThread != null) {
                stderrThread.interrupt();
                stderrThread = null;
            }
        }
    }

    public synchronized boolean isRunning() {
        return activeProcess != null;
    }

    @NonNull
    private String buildCommand(@NonNull String targetPackageName, @NonNull String scriptPath) {
        String injectPath = firstExisting(FRIDA_INJECT_PATHS);
        if (!injectPath.isEmpty()) {
            return shellQuote(injectPath)
                    + " -n " + shellQuote(targetPackageName)
                    + " -s " + shellQuote(scriptPath);
        }
        String cliPath = firstExisting(FRIDA_CLI_PATHS);
        if (!cliPath.isEmpty()) {
            return shellQuote(cliPath)
                    + " -U " + shellQuote(targetPackageName)
                    + " -l " + shellQuote(scriptPath);
        }
        return "";
    }

    @NonNull
    private String firstExisting(@NonNull String[] paths) {
        for (String path : paths) {
            try {
                if (new File(path).exists()) {
                    return path;
                }
            } catch (Exception ignored) {
                // Keep discovery best-effort.
            }
        }
        return "";
    }

    private boolean isSafePackageName(@NonNull String value) {
        return value.matches("[A-Za-z0-9_.]+");
    }

    @NonNull
    private Thread startPump(
            @NonNull String streamName,
            @NonNull InputStream stream,
            @NonNull EventSink eventSink
    ) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                    eventSink.onLine(streamName, line);
                }
            } catch (Exception exception) {
                eventSink.onLine(streamName, "Frida stream closed: " + exception.getClass().getSimpleName());
            }
        }, "root-frida-" + streamName);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    @NonNull
    private String shellQuote(@NonNull String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    public static final class ReloadResult {
        private final boolean loaded;
        private final String message;

        private ReloadResult(boolean loaded, @NonNull String message) {
            this.loaded = loaded;
            this.message = message;
        }

        public boolean isLoaded() {
            return loaded;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    public static final class StartResult {
        private final boolean scriptReady;
        private final boolean processStarted;
        private final String command;
        private final String message;

        private StartResult(
                boolean scriptReady,
                boolean processStarted,
                @NonNull String command,
                @NonNull String message
        ) {
            this.scriptReady = scriptReady;
            this.processStarted = processStarted;
            this.command = command;
            this.message = message;
        }

        public boolean isScriptReady() {
            return scriptReady;
        }

        public boolean isProcessStarted() {
            return processStarted;
        }

        @NonNull
        public String getCommand() {
            return command;
        }

        @NonNull
        public String getMessage() {
            return message;
        }
    }

    public interface EventSink {
        void onLine(@NonNull String streamName, @NonNull String line);
    }
}
