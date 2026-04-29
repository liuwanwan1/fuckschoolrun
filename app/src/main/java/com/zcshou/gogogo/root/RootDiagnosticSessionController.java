package com.acooldog.toolbox.root;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RootDiagnosticSessionController {
    private static final String DIAGNOSTIC_DIR = "root-diagnostic";

    private final Context appContext;
    private final RootDiagnosticHookScriptBuilder scriptBuilder;
    private final FridaInjectionGateway fridaInjectionGateway;
    private final List<RootDiagnosticEvent> events = new ArrayList<>();

    private String activeSessionId = "";
    private String activeTargetPackageName = "";
    private long activeStartedAtMillis;
    private List<RootDiagnosticModule> activeModules = Collections.emptyList();
    private File activeScriptFile;
    private String activeManualAttachCommand = "";
    private String activeManualSpawnCommand = "";
    private RootDiagnosticSessionReport latestReport;
    private File latestReportFile;
    private File latestJsonReportFile;
    private EventListener eventListener;

    public RootDiagnosticSessionController(@NonNull Context context) {
        this(context, new RootDiagnosticHookScriptBuilder(), new FridaInjectionGateway());
    }

    RootDiagnosticSessionController(
            @NonNull Context context,
            @NonNull RootDiagnosticHookScriptBuilder scriptBuilder,
            @NonNull FridaInjectionGateway fridaInjectionGateway
    ) {
        this.appContext = context.getApplicationContext();
        this.scriptBuilder = scriptBuilder;
        this.fridaInjectionGateway = fridaInjectionGateway;
    }

    @NonNull
    public synchronized StartResult startSession(
            @NonNull RootFeatureConfig config,
            @Nullable EventListener listener
    ) {
        if (isRunning()) {
            return new StartResult(false, activeSessionId, "诊断会话已在运行。", null);
        }
        String targetPackageName = config.getTargetPackageName().trim();
        if (targetPackageName.isEmpty()) {
            return new StartResult(false, "", "请先选择目标APK。", null);
        }
        if (!config.isEnabled(RootFeature.FRIDA_DYNAMIC_INJECTION)) {
            return new StartResult(false, "", "请先开启Frida应用级动态注入开关。", null);
        }
        List<RootDiagnosticModule> modules = RootDiagnosticModule.enabledIn(config);
        if (modules.isEmpty()) {
            return new StartResult(false, "", "请至少开启一个诊断模块。", null);
        }

        activeSessionId = "diag-" + System.currentTimeMillis();
        activeTargetPackageName = targetPackageName;
        activeStartedAtMillis = System.currentTimeMillis();
        activeModules = new ArrayList<>(modules);
        events.clear();
        eventListener = listener;
        latestReport = null;
        latestReportFile = null;
        latestJsonReportFile = null;

        try {
            File diagnosticDir = getDiagnosticDir();
            activeScriptFile = new File(diagnosticDir, activeSessionId + ".frida.js");
            String script = scriptBuilder.build(activeSessionId, targetPackageName, modules);
            writeFile(activeScriptFile, script);
            activeManualAttachCommand = scriptBuilder.buildManualAttachCommand(
                    targetPackageName,
                    activeScriptFile.getAbsolutePath()
            );
            activeManualSpawnCommand = scriptBuilder.buildManualSpawnCommand(
                    targetPackageName,
                    activeScriptFile.getAbsolutePath()
            );
            recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "script_generated",
                    "Frida脚本已生成：" + activeScriptFile.getAbsolutePath());
            recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "target_locked",
                    "目标进程限定为：" + targetPackageName);
            for (RootDiagnosticModule module : modules) {
                recordEvent(module.getId(), "module_enabled",
                        module.getTitle() + " -> " + module.getHookSurface());
            }
            FridaInjectionGateway.StartResult startResult = fridaInjectionGateway.startSession(
                    config,
                    activeScriptFile,
                    this::recordFridaLine
            );
            recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "frida_start",
                    startResult.getMessage());
            if (!startResult.getCommand().isEmpty()) {
                recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "frida_command",
                        startResult.getCommand());
            }
            return new StartResult(
                    startResult.isScriptReady(),
                    activeSessionId,
                    startResult.getMessage(),
                    activeScriptFile
            );
        } catch (Exception exception) {
            recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "start_error",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
            activeSessionId = "";
            activeTargetPackageName = "";
            activeStartedAtMillis = 0L;
            activeModules = Collections.emptyList();
            activeScriptFile = null;
            activeManualAttachCommand = "";
            activeManualSpawnCommand = "";
            eventListener = null;
            return new StartResult(false, activeSessionId, "诊断启动失败：" + exception.getMessage(), activeScriptFile);
        }
    }

    @NonNull
    public synchronized FinishResult finishSession() {
        if (!isRunning()) {
            return new FinishResult(false, "当前没有运行中的诊断会话。", latestReport, latestReportFile, latestJsonReportFile);
        }
        String stopMessage = fridaInjectionGateway.stopSession();
        recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "frida_stop", stopMessage);
        recordEvent(RootDiagnosticEvent.MODULE_REPORT, "session_finished", "开始生成目标APK诊断报告。");

        RootDiagnosticSessionReport report = new RootDiagnosticSessionReport(
                activeSessionId,
                activeTargetPackageName,
                activeStartedAtMillis,
                System.currentTimeMillis(),
                activeModules,
                events,
                activeScriptFile == null ? "" : activeScriptFile.getAbsolutePath(),
                activeManualAttachCommand,
                activeManualSpawnCommand
        );
        latestReport = report;
        try {
            File diagnosticDir = getDiagnosticDir();
            latestReportFile = new File(diagnosticDir, activeSessionId + ".report.md");
            latestJsonReportFile = new File(diagnosticDir, activeSessionId + ".report.json");
            writeFile(latestReportFile, report.toText());
            writeFile(latestJsonReportFile, report.toJson());
            recordEvent(RootDiagnosticEvent.MODULE_REPORT, "report_saved",
                    latestReportFile.getAbsolutePath());
        } catch (Exception exception) {
            recordEvent(RootDiagnosticEvent.MODULE_REPORT, "report_save_error",
                    exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }

        activeSessionId = "";
        activeTargetPackageName = "";
        activeStartedAtMillis = 0L;
        activeModules = Collections.emptyList();
        activeScriptFile = null;
        activeManualAttachCommand = "";
        activeManualSpawnCommand = "";
        eventListener = null;

        String reportPath = latestReportFile == null ? "" : latestReportFile.getAbsolutePath();
        return new FinishResult(true, "诊断会话已结束，报告已生成：" + reportPath, report, latestReportFile, latestJsonReportFile);
    }

    public synchronized boolean isRunning() {
        return !activeSessionId.isEmpty();
    }

    @NonNull
    public synchronized List<String> getRecentEventLines(int limit) {
        List<String> lines = new ArrayList<>();
        int start = Math.max(0, events.size() - Math.max(1, limit));
        for (int index = start; index < events.size(); index++) {
            lines.add(events.get(index).toDisplayLine());
        }
        return lines;
    }

    @Nullable
    public synchronized RootDiagnosticSessionReport getLatestReport() {
        return latestReport;
    }

    @Nullable
    public synchronized File getLatestReportFile() {
        return latestReportFile;
    }

    @Nullable
    public synchronized File getLatestJsonReportFile() {
        return latestJsonReportFile;
    }

    private void recordFridaLine(@NonNull String streamName, @NonNull String line) {
        RootDiagnosticEvent fridaEvent = RootDiagnosticEvent.fromFridaLine(
                activeSessionId,
                activeTargetPackageName,
                line
        );
        if (fridaEvent != null) {
            addEvent(fridaEvent);
            return;
        }
        recordEvent(RootDiagnosticEvent.MODULE_FRAMEWORK, "frida_" + streamName, line);
    }

    private void recordEvent(
            @NonNull String moduleId,
            @NonNull String type,
            @NonNull String detail
    ) {
        addEvent(RootDiagnosticEvent.local(
                activeSessionId,
                activeTargetPackageName,
                moduleId,
                type,
                detail
        ));
    }

    private synchronized void addEvent(@NonNull RootDiagnosticEvent event) {
        events.add(event);
        EventListener listener = eventListener;
        if (listener != null) {
            listener.onDiagnosticEvent(event);
        }
    }

    @NonNull
    private File getDiagnosticDir() {
        File diagnosticDir = new File(appContext.getFilesDir(), DIAGNOSTIC_DIR);
        if (!diagnosticDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            diagnosticDir.mkdirs();
        }
        return diagnosticDir;
    }

    private void writeFile(@NonNull File file, @NonNull String value) throws Exception {
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    public interface EventListener {
        void onDiagnosticEvent(@NonNull RootDiagnosticEvent event);
    }

    public static final class StartResult {
        private final boolean started;
        private final String sessionId;
        private final String message;
        private final File scriptFile;

        private StartResult(
                boolean started,
                @NonNull String sessionId,
                @NonNull String message,
                @Nullable File scriptFile
        ) {
            this.started = started;
            this.sessionId = sessionId;
            this.message = message;
            this.scriptFile = scriptFile;
        }

        public boolean isStarted() {
            return started;
        }

        @NonNull
        public String getSessionId() {
            return sessionId;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public File getScriptFile() {
            return scriptFile;
        }
    }

    public static final class FinishResult {
        private final boolean finished;
        private final String message;
        private final RootDiagnosticSessionReport report;
        private final File reportFile;
        private final File jsonReportFile;

        private FinishResult(
                boolean finished,
                @NonNull String message,
                @Nullable RootDiagnosticSessionReport report,
                @Nullable File reportFile,
                @Nullable File jsonReportFile
        ) {
            this.finished = finished;
            this.message = message;
            this.report = report;
            this.reportFile = reportFile;
            this.jsonReportFile = jsonReportFile;
        }

        public boolean isFinished() {
            return finished;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        @Nullable
        public RootDiagnosticSessionReport getReport() {
            return report;
        }

        @Nullable
        public File getReportFile() {
            return reportFile;
        }

        @Nullable
        public File getJsonReportFile() {
            return jsonReportFile;
        }
    }
}
