package com.acooldog.toolbox;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.acooldog.toolbox.algorithmkit.AlgorithmHash;
import com.acooldog.toolbox.config.InternalAuthStore;
import com.acooldog.toolbox.root.RootEnvironmentInspector;
import com.acooldog.toolbox.root.RootEnvironmentReport;
import com.acooldog.toolbox.root.RootShellProbeResult;
import com.acooldog.toolbox.root.RootTestAuditLogger;
import com.acooldog.toolbox.sensortest.EnvironmentSurfaceAnalyzer;
import com.acooldog.toolbox.sensortest.EnvironmentSurfaceReport;
import com.acooldog.toolbox.sensortest.EnvironmentSurfaceSnapshot;
import com.acooldog.toolbox.sensortest.GpxNmeaReplayEngine;
import com.acooldog.toolbox.sensortest.NmeaAnomalyMode;
import com.acooldog.toolbox.sensortest.NmeaReplayReport;
import com.acooldog.toolbox.sensortest.SensorStressConfig;
import com.acooldog.toolbox.sensortest.SensorStressEngine;
import com.acooldog.toolbox.sensortest.SensorStressMode;
import com.acooldog.toolbox.sensortest.SensorStressReport;
import com.acooldog.toolbox.share.domain.model.InternalAccountProfile;
import com.acooldog.toolbox.utils.GoUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class InternalPressureLabActivity extends BaseActivity {
    private static final String PREFS_NAME = "internal_pressure_lab";
    private static final String KEY_FUNCTION_ENABLED = "function_enabled";

    private InternalAuthStore authStore;
    private AlgorithmAuditLogger auditLogger;
    private RootTestAuditLogger rootAuditLogger;
    private RootEnvironmentInspector rootInspector;
    private AlgorithmTestFileStore fileStore;
    private SharedPreferences preferences;
    private ExecutorService executor;

    private String username = "unknown";
    private boolean sessionConfirmed;
    private boolean rootAuthorized;
    private String latestReportText;

    private CheckBox featureSwitch;
    private TextView safetyStatusView;
    private TextView rootStatusView;
    private TextView outputView;
    private TextView fileListView;
    private EditText cadenceInput;
    private EditText durationInput;
    private EditText gpxInput;
    private EditText cellLacInput;
    private EditText cellCidInput;
    private EditText wifiBssidInput;
    private EditText declaredRegionInput;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authStore = new InternalAuthStore(getApplicationContext());
        auditLogger = new AlgorithmAuditLogger(getApplicationContext());
        rootAuditLogger = new RootTestAuditLogger(getApplicationContext());
        rootInspector = new RootEnvironmentInspector(getApplicationContext());
        fileStore = new AlgorithmTestFileStore(getApplicationContext());
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();
        InternalAccountProfile profile = authStore.getProfile();
        if (profile != null && !TextUtils.isEmpty(profile.getUsername())) {
            username = profile.getUsername();
        }
        setTitle("反作弊压力测试平台");
        setContentView(buildContentView());
        refreshSafetyStatus();
        refreshGeneratedFileList();
    }

    @Override
    protected void onDestroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
        super.onDestroy();
    }

    @NonNull
    private View buildContentView() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root);

        TextView noticeView = text(
                "FOR TESTING ONLY。此平台仅生成压力测试报告与水印数据流，不写入系统服务、不注入传感器、不隐藏Root或开发者状态。",
                13,
                "#5D4037"
        );
        noticeView.setBackgroundColor(Color.parseColor("#FFF3E0"));
        noticeView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(noticeView, matchWrap());

        featureSwitch = new CheckBox(this);
        featureSwitch.setText("功能开关确认：启用本次反作弊压力测试");
        featureSwitch.setChecked(preferences.getBoolean(KEY_FUNCTION_ENABLED, false));
        featureSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean(KEY_FUNCTION_ENABLED, isChecked).apply();
            appendAuditQuietly("压力测试平台", "功能开关", "enabled=" + isChecked, "状态已记录");
            refreshSafetyStatus();
        });
        root.addView(featureSwitch, matchWrap());

        root.addView(row(
                button("确认测试会话", v -> confirmSession()),
                button("Root授权探测", v -> requestRootProbe())
        ));

        safetyStatusView = text("", 13, "#455A64");
        root.addView(safetyStatusView, matchWrap());
        rootStatusView = text("Root授权：尚未探测", 13, "#455A64");
        root.addView(rootStatusView, matchWrap());

        root.addView(sectionTitle("传感器压力测试"));
        cadenceInput = input("180", InputType.TYPE_CLASS_NUMBER);
        durationInput = input("60", InputType.TYPE_CLASS_NUMBER);
        root.addView(row(label("目标步频 SPM"), cadenceInput));
        root.addView(row(label("持续时间 秒"), durationInput));
        root.addView(row(
                button("恒定步频", v -> confirmRun("恒定步频压力测试", true, () -> runSensorScenario(SensorStressMode.CONSTANT_CADENCE))),
                button("随机波动", v -> confirmRun("随机波动压力测试", true, () -> runSensorScenario(SensorStressMode.RANDOM_FLUCTUATION)))
        ));
        root.addView(button("突变异常", v -> confirmRun("突变异常压力测试", true, () -> runSensorScenario(SensorStressMode.SPIKE_ANOMALY))), matchWrap());

        root.addView(sectionTitle("系统级GPS/NMEA测试"));
        gpxInput = input(defaultGpx(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        gpxInput.setSingleLine(false);
        gpxInput.setMinLines(5);
        root.addView(gpxInput, matchWrap());
        root.addView(row(
                button("GPX正常回放", v -> confirmRun("GPX正常回放", true, () -> runNmeaScenario(NmeaAnomalyMode.NORMAL))),
                button("信号丢失", v -> confirmRun("GPS信号丢失", true, () -> runNmeaScenario(NmeaAnomalyMode.SIGNAL_LOSS)))
        ));
        root.addView(row(
                button("卫星数突变", v -> confirmRun("卫星数突变", true, () -> runNmeaScenario(NmeaAnomalyMode.SATELLITE_COUNT_JUMP))),
                button("速度跳变", v -> confirmRun("速度跳变", true, () -> runNmeaScenario(NmeaAnomalyMode.SPEED_JUMP)))
        ));

        root.addView(sectionTitle("环境攻击面演示"));
        declaredRegionInput = input("lab-region", InputType.TYPE_CLASS_TEXT);
        cellLacInput = input("41001", InputType.TYPE_CLASS_TEXT);
        cellCidInput = input("983221", InputType.TYPE_CLASS_TEXT);
        wifiBssidInput = input("02:11:22:33:44:55", InputType.TYPE_CLASS_TEXT);
        root.addView(row(label("声明区域"), declaredRegionInput));
        root.addView(row(label("测试LAC"), cellLacInput));
        root.addView(row(label("测试CID"), cellCidInput));
        root.addView(row(label("测试BSSID"), wifiBssidInput));
        root.addView(button("生成环境检测报告", v -> confirmRun("环境攻击面报告", false, this::runEnvironmentScenario)), matchWrap());

        root.addView(sectionTitle("测试报告输出"));
        outputView = text("尚未生成测试报告。", 13, "#263238");
        outputView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        outputView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(outputView, matchWrap());
        root.addView(button("保存最新测试报告", v -> saveLatestReport()), matchWrap());

        root.addView(sectionTitle("测试数据管理"));
        fileListView = text("", 12, "#455A64");
        fileListView.setBackgroundColor(Color.parseColor("#F5F7FA"));
        fileListView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(fileListView, matchWrap());
        return scrollView;
    }

    private void confirmSession() {
        if (!baseSafety()) {
            GoUtils.DisplayToast(this, "安全检查未通过：需要DEBUG构建、内测账号和功能开关。");
            refreshSafetyStatus();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认压力测试会话")
                .setMessage("本会话仅生成测试报告与水印数据，不执行系统注入、Hook加载、信号伪造或隐藏能力。是否继续？")
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    sessionConfirmed = true;
                    appendAuditQuietly("压力测试平台", "会话确认", "user=" + username, "会话已确认");
                    if (rootAuditLogger != null) {
                        rootAuditLogger.append("确认反作弊压力测试会话");
                    }
                    refreshSafetyStatus();
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void requestRootProbe() {
        if (!baseSafety() || !sessionConfirmed) {
            GoUtils.DisplayToast(this, "请先启用功能并确认测试会话。");
            refreshSafetyStatus();
            return;
        }
        rootStatusView.setText("Root授权：正在执行 su -c id 探测…");
        executor.execute(() -> {
            RootShellProbeResult result = rootInspector.requestRootShellProbe();
            if (rootAuditLogger != null) {
                rootAuditLogger.append("压力测试Root探测: authorized=" + result.isAuthorized()
                        + ", timedOut=" + result.isTimedOut()
                        + ", exitCode=" + result.getExitCode()
                        + ", output=" + result.getOutput());
            }
            appendAuditQuietly(
                    "压力测试平台",
                    "Root授权探测",
                    "su -c id",
                    "authorized=" + result.isAuthorized() + ",exit=" + result.getExitCode()
            );
            runOnUiThread(() -> {
                rootAuthorized = result.isAuthorized();
                rootStatusView.setText(buildRootStatus(result));
                refreshSafetyStatus();
            });
        });
    }

    private void confirmRun(@NonNull String title, boolean requireRoot, @NonNull Runnable action) {
        if (!checkSafety(requireRoot)) {
            GoUtils.DisplayToast(this, requireRoot
                    ? "安全检查未通过：需要DEBUG、内测账号、功能开关、会话确认和Root授权探测通过。"
                    : "安全检查未通过：需要DEBUG、内测账号、功能开关和会话确认。");
            refreshSafetyStatus();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认运行测试")
                .setMessage(title + " 将只在应用内生成 FOR TESTING ONLY 报告，不会写入系统或修改设备状态。")
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> action.run())
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private void runSensorScenario(@NonNull SensorStressMode mode) {
        try {
            int cadence = parseInt(cadenceInput, "目标步频");
            int duration = parseInt(durationInput, "持续时间");
            SensorStressReport report = new SensorStressEngine().generate(
                    SensorStressConfig.create(mode, cadence, duration, System.currentTimeMillis())
            );
            String hash = AlgorithmHash.sha256(report.toJson());
            latestReportText = report.toHumanReport() + "hash=" + hash + "\n\n" + report.toCsv();
            outputView.setText(report.toHumanReport() + "hash=" + hash);
            appendAuditQuietly("传感器压力测试", "生成报告", "mode=" + mode + ",cadence=" + cadence + ",duration=" + duration,
                    report.summary() + ",hash=" + hash);
            if (rootAuditLogger != null) {
                rootAuditLogger.append("传感器压力测试报告: " + report.summary() + ",hash=" + hash);
            }
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void runNmeaScenario(@NonNull NmeaAnomalyMode mode) {
        try {
            NmeaReplayReport report = new GpxNmeaReplayEngine().generate(gpxInput.getText().toString(), mode);
            String nmeaStream = report.toNmeaStream();
            String hash = AlgorithmHash.sha256(report.toJson() + nmeaStream);
            latestReportText = report.toHumanReport() + "hash=" + hash + "\n\n" + nmeaStream;
            outputView.setText(report.toHumanReport() + "hash=" + hash);
            appendAuditQuietly("NMEA压力测试", "生成报告", "mode=" + mode, report.summary() + ",hash=" + hash);
            if (rootAuditLogger != null) {
                rootAuditLogger.append("NMEA压力测试报告: " + report.summary() + ",hash=" + hash);
            }
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void runEnvironmentScenario() {
        try {
            RootEnvironmentReport rootReport = rootInspector.inspect();
            EnvironmentSurfaceSnapshot snapshot = new EnvironmentSurfaceSnapshot(
                    !rootReport.getRootManagerPackages().isEmpty(),
                    !rootReport.getSuBinaryPaths().isEmpty(),
                    !rootReport.getHookFrameworkPackages().isEmpty(),
                    rootReport.isDeveloperOptionsEnabled(),
                    rootReport.isMockLocationAllowedForThisApp(),
                    cellLacInput.getText().toString(),
                    cellCidInput.getText().toString(),
                    wifiBssidInput.getText().toString(),
                    declaredRegionInput.getText().toString()
            );
            EnvironmentSurfaceReport report = new EnvironmentSurfaceAnalyzer().analyze(snapshot);
            String hash = AlgorithmHash.sha256(report.toJson());
            latestReportText = report.toHumanReport() + "hash=" + hash + "\n\n" + report.toJson();
            outputView.setText(report.toHumanReport() + "hash=" + hash);
            appendAuditQuietly("环境攻击面检测", "生成报告", rootReport.summarizeForAudit(), report.summary() + ",hash=" + hash);
            if (rootAuditLogger != null) {
                rootAuditLogger.append("环境攻击面检测报告: " + report.summary() + ",hash=" + hash);
            }
        } catch (Exception exception) {
            GoUtils.DisplayToast(this, exception.getMessage());
        }
    }

    private void saveLatestReport() {
        if (TextUtils.isEmpty(latestReportText)) {
            GoUtils.DisplayToast(this, "当前没有可保存的测试报告。");
            return;
        }
        if (!checkSafety(false)) {
            GoUtils.DisplayToast(this, "安全检查未通过，无法保存报告。");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认保存测试报告")
                .setMessage("报告将保存到应用私有测试目录，并写入加密审计日志。")
                .setPositiveButton(R.string.route_link_settings_confirm, (dialog, which) -> {
                    try {
                        File file = fileStore.save("pressure_report", "txt", latestReportText);
                        String hash = AlgorithmHash.sha256(latestReportText);
                        appendAuditQuietly("压力测试平台", "保存报告", file.getName(), "bytes=" + file.length() + ",hash=" + hash);
                        refreshGeneratedFileList();
                        GoUtils.DisplayToast(this, "测试报告已保存：" + file.getName());
                    } catch (Exception exception) {
                        GoUtils.DisplayToast(this, exception.getMessage());
                    }
                })
                .setNegativeButton(R.string.route_link_settings_cancel, null)
                .show();
    }

    private boolean baseSafety() {
        return BuildConfig.DEBUG
                && BuildConfig.ENABLE_ALGORITHM_TEST
                && BuildConfig.INTERNAL_ROOT_TESTING_ENABLED
                && authStore != null
                && authStore.isLoggedIn()
                && featureSwitch != null
                && featureSwitch.isChecked();
    }

    private boolean checkSafety(boolean requireRoot) {
        return baseSafety() && sessionConfirmed && (!requireRoot || rootAuthorized);
    }

    private void refreshSafetyStatus() {
        safetyStatusView.setText(String.format(
                Locale.getDefault(),
                "安全状态：DEBUG=%s，算法开关=%s，Root内测=%s，内测用户=%s，功能确认=%s，会话确认=%s，Root探测=%s",
                BuildConfig.DEBUG,
                BuildConfig.ENABLE_ALGORITHM_TEST,
                BuildConfig.INTERNAL_ROOT_TESTING_ENABLED,
                authStore != null && authStore.isLoggedIn() ? username : "未登录",
                featureSwitch != null && featureSwitch.isChecked(),
                sessionConfirmed,
                rootAuthorized
        ));
    }

    private String buildRootStatus(@NonNull RootShellProbeResult result) {
        if (result.isTimedOut()) {
            return "Root授权：探测超时";
        }
        String output = TextUtils.isEmpty(result.getOutput()) ? "" : "\n输出：" + result.getOutput();
        return "Root授权：authorized=" + result.isAuthorized() + ", exit=" + result.getExitCode() + output;
    }

    private void refreshGeneratedFileList() {
        List<File> files = fileStore.listGeneratedFiles();
        if (files.isEmpty()) {
            fileListView.setText("暂无已生成测试用例。");
            return;
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(files.size(), 20);
        for (int index = 0; index < limit; index++) {
            File file = files.get(index);
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(file.getName()).append(" | ").append(file.length()).append(" bytes");
        }
        fileListView.setText(builder.toString());
    }

    private void appendAuditQuietly(
            @NonNull String module,
            @NonNull String action,
            @NonNull String input,
            @NonNull String output
    ) {
        try {
            auditLogger.append(username, module, action, input, output);
        } catch (Exception ignored) {
            // Keep report generation usable even if local audit encryption is unavailable.
        }
    }

    private int parseInt(@NonNull EditText input, @NonNull String label) {
        try {
            return Integer.parseInt(input.getText().toString().trim());
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "无效");
        }
    }

    private String defaultGpx() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<gpx version=\"1.1\" creator=\"internal-pressure-lab\">\n"
                + "  <trk><trkseg>\n"
                + "    <trkpt lat=\"36.667662\" lon=\"117.027707\"><ele>55</ele><time>2024-01-01T00:00:00Z</time></trkpt>\n"
                + "    <trkpt lat=\"36.667900\" lon=\"117.028100\"><ele>56</ele><time>2024-01-01T00:00:05Z</time></trkpt>\n"
                + "    <trkpt lat=\"36.668200\" lon=\"117.028500\"><ele>57</ele><time>2024-01-01T00:00:10Z</time></trkpt>\n"
                + "  </trkseg></trk>\n"
                + "</gpx>";
    }

    private TextView sectionTitle(@NonNull String value) {
        TextView view = text(value, 16, "#1F2A33");
        view.setGravity(Gravity.START);
        view.setPadding(0, dp(18), 0, dp(6));
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private TextView label(@NonNull String value) {
        TextView view = text(value, 14, "#455A64");
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private EditText input(@NonNull String value, int inputType) {
        EditText editText = new EditText(this);
        editText.setSingleLine(true);
        editText.setInputType(inputType);
        editText.setText(value);
        return editText;
    }

    private Button button(@NonNull String value, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(value);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout row(@NonNull View left, @NonNull View right) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    private TextView text(@NonNull String value, int sp, String color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(Color.parseColor(color));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
