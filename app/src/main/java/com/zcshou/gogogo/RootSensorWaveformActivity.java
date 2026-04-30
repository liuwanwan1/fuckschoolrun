package com.acooldog.toolbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.acooldog.toolbox.root.RootDiagnosticSettings;
import com.acooldog.toolbox.root.RootDiagnosticSettingsStore;
import com.acooldog.toolbox.root.RootSensorMotionProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RootSensorWaveformActivity extends BaseActivity {
    private static final int RECORD_SAMPLE_COUNT = 48;
    private static final long RECORD_DURATION_MILLIS = 30000L;
    private static final long MOTION_WAIT_TIMEOUT_MILLIS = 10000L;
    private static final double MOTION_START_DELTA = 1.25d;

    private RootDiagnosticSettingsStore settingsStore;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private EditText minCadenceInput;
    private EditText maxCadenceInput;
    private EditText waveAmplitudeInput;
    private EditText jitterRangeInput;
    private EditText jitterProbabilityInput;
    private TextView statusView;
    private TextView waveformSummaryView;
    private TextView countdownView;
    private WaveformEditorView waveformView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Double> recordedMagnitudes = new ArrayList<>();
    private final List<Long> recordedTimestamps = new ArrayList<>();
    private final Runnable stopRecordingRunnable = () -> stopRecording(true);
    private boolean recording;
    private boolean motionRecordingStarted;
    private long recordingStartMillis;

    private final SensorEventListener recordingListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!recording || event == null || event.values == null || event.values.length < 3) {
                return;
            }
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            double magnitude = Math.sqrt(x * x + y * y + z * z);
            long now = System.currentTimeMillis();
            if (!motionRecordingStarted) {
                if (Math.abs(magnitude - SensorManager.GRAVITY_EARTH) < MOTION_START_DELTA) {
                    return;
                }
                motionRecordingStarted = true;
                recordingStartMillis = now;
                handler.removeCallbacks(stopRecordingRunnable);
                handler.postDelayed(stopRecordingRunnable, RECORD_DURATION_MILLIS);
                countdownView.setText("录入中");
                statusView.setText("已检测到运动，正在录入步频和加速度波形，最长30秒。");
            }
            recordedMagnitudes.add(magnitude);
            recordedTimestamps.add(now);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No-op for recording.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsStore = new RootDiagnosticSettingsStore(getApplicationContext());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        buildContent();
        loadSettings();
    }

    @Override
    protected void onPause() {
        stopRecording(false);
        super.onPause();
    }

    private void buildContent() {
        FrameLayout root = new FrameLayout(this);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(16f);
        content.setPadding(padding, padding, padding, padding + dp(24f));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton backButton = new ImageButton(this);
        backButton.setImageResource(android.R.drawable.ic_media_previous);
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setContentDescription("返回");
        backButton.setOnClickListener(v -> finish());
        toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(44f), dp(44f)));

        TextView titleView = new TextView(this);
        titleView.setText("传感器运动波形");
        titleView.setTextColor(Color.parseColor("#263238"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.addView(titleView, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        content.addView(toolbar);

        statusView = addText(content, "", 14f, "#455A64");
        addLabel(content, "最低步频 SPM");
        minCadenceInput = addInput(content);
        addLabel(content, "最高步频 SPM");
        maxCadenceInput = addInput(content);
        addLabel(content, "Z轴波形振幅");
        waveAmplitudeInput = addInput(content);
        addLabel(content, "自然抖动范围 m/s²");
        jitterRangeInput = addInput(content);
        addLabel(content, "自然抖动频率/概率 0-1");
        jitterProbabilityInput = addInput(content);

        waveformSummaryView = addText(content, "", 13f, "#607D8B");
        waveformView = new WaveformEditorView(this);
        content.addView(waveformView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220f)
        ));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        actions.addView(actionButton("录入运动步频SPM（最长30秒）", v -> startCountdown(3)));
        actions.addView(actionButton("恢复默认波形", v -> applyDefaultWaveform()));
        actions.addView(actionButton("保存传感器设置", v -> saveSettings()));
        content.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        countdownView = new TextView(this);
        countdownView.setVisibility(View.GONE);
        countdownView.setGravity(Gravity.CENTER);
        countdownView.setTextColor(Color.WHITE);
        countdownView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 76f);
        countdownView.setBackgroundColor(Color.parseColor("#CC263238"));
        root.addView(scrollView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(countdownView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(root);
    }

    private void loadSettings() {
        RootDiagnosticSettings settings = settingsStore.load();
        RootSensorMotionProfile profile = settings.getSensorMotionProfile();
        minCadenceInput.setText(String.format(Locale.US, "%.2f", settings.getSensorMinCadence()));
        maxCadenceInput.setText(String.format(Locale.US, "%.2f", settings.getSensorMaxCadence()));
        waveAmplitudeInput.setText(String.format(Locale.US, "%.2f", settings.getSensorWaveAmplitude()));
        jitterRangeInput.setText(String.format(Locale.US, "%.2f", profile.getNaturalJitterRange()));
        jitterProbabilityInput.setText(String.format(Locale.US, "%.2f", profile.getNaturalJitterProbability()));
        waveformView.setSamples(profile.getWaveformSamples());
        statusView.setText(String.format(
                Locale.getDefault(),
                "当前步频范围 %.0f-%.0f SPM，Z轴振幅 %.1f。可自动录入自身步频，也可拖动波形点编辑一个步频周期。",
                settings.getSensorMinCadence(),
                settings.getSensorMaxCadence(),
                settings.getSensorWaveAmplitude()
        ));
        updateWaveformSummary();
    }

    private void startCountdown(int secondsLeft) {
        if (recording) {
            return;
        }
        if (accelerometer == null || sensorManager == null) {
            Toast.makeText(this, "当前设备没有可用的加速度计。", Toast.LENGTH_SHORT).show();
            return;
        }
        countdownView.setVisibility(View.VISIBLE);
        countdownView.setText(String.valueOf(secondsLeft));
        if (secondsLeft <= 1) {
            handler.postDelayed(this::startRecording, 1000L);
            return;
        }
        handler.postDelayed(() -> startCountdown(secondsLeft - 1), 1000L);
    }

    private void startRecording() {
        recordedMagnitudes.clear();
        recordedTimestamps.clear();
        recording = true;
        motionRecordingStarted = false;
        recordingStartMillis = 0L;
        countdownView.setText("等待运动");
        statusView.setText("倒计时结束，请开始自然运动；检测到运动后自动开始录入，最长30秒。");
        sensorManager.registerListener(recordingListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        handler.postDelayed(stopRecordingRunnable, MOTION_WAIT_TIMEOUT_MILLIS);
    }

    private void stopRecording(boolean applyResult) {
        handler.removeCallbacksAndMessages(null);
        if (recording && sensorManager != null) {
            sensorManager.unregisterListener(recordingListener);
        }
        boolean wasRecording = recording;
        recording = false;
        countdownView.setVisibility(View.GONE);
        if (!applyResult || !wasRecording) {
            return;
        }
        if (!motionRecordingStarted || recordedMagnitudes.size() < 8) {
            statusView.setText("录入结束，但没有检测到足够的运动数据。请重新录入。");
            return;
        }
        List<Double> samples = RootSensorMotionProfile.normalizeRecordedMagnitudes(
                recordedMagnitudes,
                RECORD_SAMPLE_COUNT
        );
        waveformView.setSamples(samples);
        boolean cadenceUpdated = applyCadenceAndJitterAnalysis();
        statusView.setText(cadenceUpdated
                ? "录入完成，已自动填入步频和自然抖动参数，可继续编辑后保存。"
                : "录入完成，已自动填入自然抖动参数；未检测到稳定步频，请重新录入或手动填写SPM。");
    }

    private void applyDefaultWaveform() {
        RootSensorMotionProfile profile = RootSensorMotionProfile.defaults();
        minCadenceInput.setText(String.format(Locale.US, "%.2f", 172d));
        maxCadenceInput.setText(String.format(Locale.US, "%.2f", 182d));
        waveAmplitudeInput.setText(String.format(Locale.US, "%.2f", 3.5d));
        jitterRangeInput.setText(String.format(Locale.US, "%.2f", profile.getNaturalJitterRange()));
        jitterProbabilityInput.setText(String.format(Locale.US, "%.2f", profile.getNaturalJitterProbability()));
        waveformView.setSamples(profile.getWaveformSamples());
        updateWaveformSummary();
    }

    private void saveSettings() {
        try {
            RootSensorMotionProfile profile = new RootSensorMotionProfile(
                    parseDouble(jitterRangeInput, "自然抖动范围"),
                    parseDouble(jitterProbabilityInput, "自然抖动概率"),
                    waveformView.getSamples()
            );
            RootDiagnosticSettings current = settingsStore.load();
            RootDiagnosticSettings next = current.withSensor(
                    parseDouble(minCadenceInput, "最低步频"),
                    parseDouble(maxCadenceInput, "最高步频"),
                    parseDouble(waveAmplitudeInput, "Z轴波形振幅"),
                    profile
            );
            settingsStore.save(next);
            updateWaveformSummary();
            Toast.makeText(this, "传感器设置已保存。", Toast.LENGTH_SHORT).show();
        } catch (IllegalArgumentException exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean applyCadenceAndJitterAnalysis() {
        CadenceAnalysis cadence = analyzeCadence(recordedMagnitudes, recordedTimestamps);
        if (cadence.valid) {
            minCadenceInput.setText(String.format(Locale.US, "%.2f", cadence.minSpm));
            maxCadenceInput.setText(String.format(Locale.US, "%.2f", cadence.maxSpm));
        }
        double average = average(recordedMagnitudes);
        double standardDeviation = standardDeviation(recordedMagnitudes, average);
        double activeThreshold = Math.max(0.35d, standardDeviation * 0.7d);
        int activeSamples = 0;
        for (double value : recordedMagnitudes) {
            if (Math.abs(value - average) >= activeThreshold) {
                activeSamples++;
            }
        }
        double jitterRange = clampDouble(standardDeviation, 0d, 3d);
        double jitterProbability = recordedMagnitudes.isEmpty()
                ? 0d
                : clampDouble(activeSamples / (double) recordedMagnitudes.size(), 0d, 1d);
        jitterRangeInput.setText(String.format(Locale.US, "%.2f", jitterRange));
        jitterProbabilityInput.setText(String.format(Locale.US, "%.2f", jitterProbability));
        if (cadence.valid) {
            waveformSummaryView.setText(String.format(
                    Locale.getDefault(),
                    "波形采样点：%d，检测步频 %.0f-%.0f SPM，抖动 %.2f@%.0f%%。",
                    waveformView.getSamples().size(),
                    cadence.minSpm,
                    cadence.maxSpm,
                    jitterRange,
                    jitterProbability * 100d
            ));
        }
        return cadence.valid;
    }

    @NonNull
    private CadenceAnalysis analyzeCadence(
            @NonNull List<Double> magnitudes,
            @NonNull List<Long> timestamps
    ) {
        if (magnitudes.size() < 6 || magnitudes.size() != timestamps.size()) {
            return CadenceAnalysis.invalid();
        }
        double average = average(magnitudes);
        double standardDeviation = standardDeviation(magnitudes, average);
        double threshold = average + Math.max(0.45d, standardDeviation * 0.35d);
        List<Long> peaks = new ArrayList<>();
        long lastPeakAt = -1L;
        for (int index = 1; index < magnitudes.size() - 1; index++) {
            double previous = magnitudes.get(index - 1);
            double current = magnitudes.get(index);
            double next = magnitudes.get(index + 1);
            long at = timestamps.get(index);
            if (current < threshold || current < previous || current < next) {
                continue;
            }
            if (lastPeakAt >= 0L && at - lastPeakAt < 260L) {
                continue;
            }
            peaks.add(at);
            lastPeakAt = at;
        }
        List<Double> cadences = new ArrayList<>();
        for (int index = 1; index < peaks.size(); index++) {
            long intervalMillis = peaks.get(index) - peaks.get(index - 1);
            if (intervalMillis <= 0L) {
                continue;
            }
            double spm = 60000d / intervalMillis;
            if (spm >= 80d && spm <= 260d) {
                cadences.add(spm);
            }
        }
        if (cadences.isEmpty()) {
            return CadenceAnalysis.invalid();
        }
        double min = cadences.get(0);
        double max = cadences.get(0);
        for (double cadence : cadences) {
            min = Math.min(min, cadence);
            max = Math.max(max, cadence);
        }
        min = clampDouble(min, RootDiagnosticSettings.MIN_SENSOR_CADENCE, RootDiagnosticSettings.MAX_SENSOR_CADENCE);
        max = clampDouble(max, RootDiagnosticSettings.MIN_SENSOR_CADENCE, RootDiagnosticSettings.MAX_SENSOR_CADENCE);
        return new CadenceAnalysis(Math.min(min, max), Math.max(min, max), true);
    }

    private void updateWaveformSummary() {
        waveformSummaryView.setText("波形采样点：" + waveformView.getSamples().size()
                + "，用于按当前步频循环注入。");
    }

    private double average(@NonNull List<Double> values) {
        if (values.isEmpty()) {
            return 0d;
        }
        double total = 0d;
        for (double value : values) {
            total += value;
        }
        return total / values.size();
    }

    private double standardDeviation(@NonNull List<Double> values, double average) {
        if (values.isEmpty()) {
            return 0d;
        }
        double variance = 0d;
        for (double value : values) {
            double delta = value - average;
            variance += delta * delta;
        }
        return Math.sqrt(variance / values.size());
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    @NonNull
    private TextView addText(@NonNull LinearLayout parent, @NonNull String text, float sp, @NonNull String color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(Color.parseColor(color));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        textView.setLineSpacing(dp(2f), 1.0f);
        parent.addView(textView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return textView;
    }

    private void addLabel(@NonNull LinearLayout parent, @NonNull String text) {
        TextView label = addText(parent, text, 13f, "#455A64");
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) label.getLayoutParams();
        params.topMargin = dp(10f);
        label.setLayoutParams(params);
    }

    @NonNull
    private EditText addInput(@NonNull LinearLayout parent) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        parent.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return input;
    }

    @NonNull
    private Button actionButton(@NonNull String label, @NonNull View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(8f);
        button.setLayoutParams(params);
        return button;
    }

    private double parseDouble(@NonNull EditText input, @NonNull String label) {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (TextUtils.isEmpty(value)) {
            throw new IllegalArgumentException(label + "不能为空。");
        }
        try {
            return Double.parseDouble(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException(label + "必须是数字。");
        }
    }

    private int dp(float value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    private static final class CadenceAnalysis {
        private final double minSpm;
        private final double maxSpm;
        private final boolean valid;

        private CadenceAnalysis(double minSpm, double maxSpm, boolean valid) {
            this.minSpm = minSpm;
            this.maxSpm = maxSpm;
            this.valid = valid;
        }

        @NonNull
        private static CadenceAnalysis invalid() {
            return new CadenceAnalysis(0d, 0d, false);
        }
    }

    private static final class WaveformEditorView extends View {
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private List<Double> samples = new ArrayList<>(RootSensorMotionProfile.defaults().getWaveformSamples());

        private WaveformEditorView(Context context) {
            super(context);
            gridPaint.setColor(Color.parseColor("#CFD8DC"));
            gridPaint.setStrokeWidth(1.5f);
            linePaint.setColor(Color.parseColor("#00796B"));
            linePaint.setStrokeWidth(4f);
            linePaint.setStyle(Paint.Style.STROKE);
            pointPaint.setColor(Color.parseColor("#004D40"));
            pointPaint.setStyle(Paint.Style.FILL);
            setBackgroundColor(Color.parseColor("#F7FAFA"));
        }

        private void setSamples(@NonNull List<Double> nextSamples) {
            samples = new ArrayList<>(new RootSensorMotionProfile(0d, 0d, nextSamples).getWaveformSamples());
            invalidate();
        }

        @NonNull
        private List<Double> getSamples() {
            return new ArrayList<>(samples);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();
            int left = getPaddingLeft() + 8;
            int right = width - getPaddingRight() - 8;
            int top = getPaddingTop() + 12;
            int bottom = height - getPaddingBottom() - 12;
            int centerY = (top + bottom) / 2;
            canvas.drawLine(left, centerY, right, centerY, gridPaint);
            canvas.drawLine(left, top, left, bottom, gridPaint);
            canvas.drawLine(right, top, right, bottom, gridPaint);
            if (samples.size() < 2 || right <= left || bottom <= top) {
                return;
            }
            path.reset();
            for (int index = 0; index < samples.size(); index++) {
                float x = left + (right - left) * index / (float) (samples.size() - 1);
                float y = valueToY(samples.get(index), top, bottom);
                if (index == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }
            canvas.drawPath(path, linePaint);
            for (int index = 0; index < samples.size(); index++) {
                float x = left + (right - left) * index / (float) (samples.size() - 1);
                float y = valueToY(samples.get(index), top, bottom);
                canvas.drawCircle(x, y, 5f, pointPaint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    || event.getAction() == MotionEvent.ACTION_MOVE) {
                updateSampleFromTouch(event.getX(), event.getY());
                return true;
            }
            return true;
        }

        private void updateSampleFromTouch(float x, float y) {
            if (samples.size() < 2) {
                return;
            }
            int left = getPaddingLeft() + 8;
            int right = getWidth() - getPaddingRight() - 8;
            int top = getPaddingTop() + 12;
            int bottom = getHeight() - getPaddingBottom() - 12;
            if (right <= left || bottom <= top) {
                return;
            }
            int index = Math.round((x - left) * (samples.size() - 1) / (float) (right - left));
            index = Math.max(0, Math.min(samples.size() - 1, index));
            double value = 1d - ((y - top) / Math.max(1f, bottom - top)) * 2d;
            value = Math.max(-1d, Math.min(1d, value));
            samples.set(index, value);
            invalidate();
        }

        private float valueToY(double value, int top, int bottom) {
            double clamped = Math.max(-1d, Math.min(1d, value));
            return (float) (top + (1d - clamped) * 0.5d * (bottom - top));
        }
    }
}
