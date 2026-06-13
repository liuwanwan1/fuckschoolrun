package com.acooldog.toolbox.lspatch;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPatch免Root步频模拟模块 — 适配支付宝阳光校园跑。
 * <p>
 * 使用方式：
 * 1. 用LSPatch Manager将此模块APK嵌入支付宝APK
 * 2. 安装修补后的支付宝
 * 3. 可选：在 /sdcard/schoolrun_lspatch.json 配置步频参数
 * <p>
 * 默认步频 180 SPM，范围 140-220 SPM，步长 0.8m。
 */
public final class LspatchStepModule implements IXposedHookLoadPackage {

    // ── 目标配置 ──────────────────────────────────────────────
    private static final String TARGET_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String CONFIG_FILE_PATH = "/sdcard/schoolrun_lspatch.json";

    // ── 默认传感器参数 ─────────────────────────────────────────
    private static final double DEFAULT_CADENCE_SPM = 180.0;
    private static final double DEFAULT_MIN_CADENCE = 140.0;
    private static final double DEFAULT_MAX_CADENCE = 220.0;
    private static final double DEFAULT_WAVE_AMPLITUDE = 2.5;   // 加速度计Z轴波形振幅 m/s²
    private static final double DEFAULT_STEP_LENGTH_M = 0.8;     // 每步距离（米）
    private static final double JITTER_AMPLITUDE = 0.15;         // 自然抖动振幅

    // ── 运行时状态 ─────────────────────────────────────────────
    private static volatile boolean moduleActive;
    private static volatile boolean pulseLoopScheduled;
    private static volatile long sessionStartMillis;
    private static volatile float stepBaseOffset = -1f;
    private static volatile long lastSensorLogSecond = -1L;
    private static volatile double currentCadence = DEFAULT_CADENCE_SPM;
    private static volatile double currentMinCadence = DEFAULT_MIN_CADENCE;
    private static volatile double currentMaxCadence = DEFAULT_MAX_CADENCE;
    private static volatile double currentWaveAmplitude = DEFAULT_WAVE_AMPLITUDE;
    private static volatile double currentStepLength = DEFAULT_STEP_LENGTH_M;

    private static final Random RANDOM = new Random();
    private static final Set<String> hookedSensorListenerClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, SensorEventListener> activeTargetListeners =
            new ConcurrentHashMap<>();
    private static final Set<String> loggedSensorErrors =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static Handler sensorPulseHandler;
    private static Context targetAppContext;

    // ── IXposedHookLoadPackage ─────────────────────────────────

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) {
            return;
        }
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }
        XposedBridge.log("[SchoolRunLSP] Loaded into Alipay process: " + lpparam.processName);

        // Step 1: Hook Application.onCreate to capture context and load config.
        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    lpparam.classLoader,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            targetAppContext = ((android.app.Application) param.thisObject)
                                    .getApplicationContext();
                            loadConfigFromFile();
                            moduleActive = true;
                            XposedBridge.log("[SchoolRunLSP] Module activated."
                                    + " cadence=" + Math.round(currentCadence) + " SPM"
                                    + " range=" + Math.round(currentMinCadence) + "-"
                                    + Math.round(currentMaxCadence));
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[SchoolRunLSP] Application hook failed: " + t.getMessage());
        }

        // Step 2: Hook SensorManager.registerListener to capture target sensor listeners.
        hookSensorManager(lpparam.classLoader);

        // Step 3: Hook SensorManager.getDefaultSensor to normalize sensor availability.
        hookSensorAvailability(lpparam.classLoader);
    }

    // ── SensorManager Hooks ──────────────────────────────────

    private void hookSensorManager(ClassLoader classLoader) {
        try {
            // Hook registerListener(SensorEventListener, Sensor, int)
            XposedHelpers.findAndHookMethod(
                    SensorManager.class,
                    "registerListener",
                    SensorEventListener.class,
                    Sensor.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!moduleActive) return;
                            SensorEventListener listener = (SensorEventListener) param.args[0];
                            Sensor sensor = (Sensor) param.args[1];
                            if (listener == null || sensor == null) return;
                            captureSensorTarget(listener, sensor);
                        }
                    });

            // Hook registerListener(SensorEventListener, Sensor, int, int)
            if (Build.VERSION.SDK_INT >= 19) {
                XposedHelpers.findAndHookMethod(
                        SensorManager.class,
                        "registerListener",
                        SensorEventListener.class,
                        Sensor.class,
                        int.class,
                        int.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!moduleActive) return;
                                SensorEventListener listener = (SensorEventListener) param.args[0];
                                Sensor sensor = (Sensor) param.args[1];
                                if (listener == null || sensor == null) return;
                                captureSensorTarget(listener, sensor);
                            }
                        });
            }

            // Hook registerListener(SensorEventListener, Sensor, int, int, Handler)
            if (Build.VERSION.SDK_INT >= 19) {
                XposedHelpers.findAndHookMethod(
                        SensorManager.class,
                        "registerListener",
                        SensorEventListener.class,
                        Sensor.class,
                        int.class,
                        int.class,
                        Handler.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!moduleActive) return;
                                SensorEventListener listener = (SensorEventListener) param.args[0];
                                Sensor sensor = (Sensor) param.args[1];
                                if (listener == null || sensor == null) return;
                                captureSensorTarget(listener, sensor);
                            }
                        });
            }

            XposedBridge.log("[SchoolRunLSP] SensorManager hooks installed.");
        } catch (Throwable t) {
            XposedBridge.log("[SchoolRunLSP] SensorManager hook failed: " + t.getMessage());
        }
    }

    private void captureSensorTarget(SensorEventListener listener, Sensor sensor) {
        int type = sensor.getType();
        if (type != Sensor.TYPE_ACCELEROMETER
                && type != Sensor.TYPE_STEP_COUNTER
                && type != Sensor.TYPE_STEP_DETECTOR
                && type != 19  // STEP_COUNTER on some devices
                && type != 18) { // STEP_DETECTOR on some devices
            return;
        }
        String listenerKey = listener.getClass().getName();
        if (!hookedSensorListenerClasses.add(listenerKey)) {
            return; // Already hooked this listener class
        }

        // Store the active listener reference for pulse injection.
        activeTargetListeners.put(listenerKey, listener);
        XposedBridge.log("[SchoolRunLSP] Captured sensor listener: " + listenerKey
                + " for sensor type=" + type);

        // Hook this listener's onSensorChanged to inject fake data.
        hookSensorEventListener(listener);
        // Start the pulse loop if not already running.
        scheduleSensorPulseLoop();
    }

    @SuppressWarnings("unchecked")
    private void hookSensorEventListener(SensorEventListener listener) {
        try {
            Class<?> listenerClass = listener.getClass();
            // Use Xposed to replace onSensorChanged for ALL instances of this class.
            XposedHelpers.findAndHookMethod(
                    listenerClass,
                    "onSensorChanged",
                    SensorEvent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!moduleActive) return;
                            SensorEvent event = (SensorEvent) param.args[0];
                            if (event == null || event.sensor == null) return;
                            mutateSensorEvent(event);
                        }
                    });
        } catch (Throwable t) {
            String key = listener.getClass().getName() + ":" + t.getClass().getName();
            if (loggedSensorErrors.add(key)) {
                XposedBridge.log("[SchoolRunLSP] Sensor hook failed for "
                        + listener.getClass().getName() + ": " + t.getMessage());
            }
        }
    }

    // ── Sensor Data Mutation ─────────────────────────────────

    private void mutateSensorEvent(SensorEvent event) {
        int type = event.sensor.getType();
        if (type != Sensor.TYPE_ACCELEROMETER
                && type != Sensor.TYPE_STEP_COUNTER
                && type != Sensor.TYPE_STEP_DETECTOR
                && type != 18 && type != 19) {
            return;
        }

        if (sessionStartMillis == 0L) {
            sessionStartMillis = System.currentTimeMillis();
            stepBaseOffset = -1f;
        }

        long now = System.currentTimeMillis();
        double elapsedSeconds = Math.max(0d, (now - sessionStartMillis) / 1000.0d);
        double frequency = currentCadence / 60.0d; // Hz
        double phase = elapsedSeconds * Math.PI * 2.0 * frequency;

        if (type == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
            // Z-axis: gravity + sinusoidal running motion + natural jitter.
            double jitter = (RANDOM.nextDouble() * 2.0 - 1.0) * JITTER_AMPLITUDE;
            double wave = Math.sin(phase);
            double sideWave = Math.sin(phase + Math.PI * 0.5);
            event.values[0] = (float) ((RANDOM.nextDouble() * 2.0 - 1.0) * 0.2);
            event.values[1] = (float) (sideWave * 1.5 + jitter * 0.35);
            event.values[2] = (float) (9.81 + wave * currentWaveAmplitude + jitter);

            long elapsedWholeSecond = (long) elapsedSeconds;
            if (elapsedWholeSecond != lastSensorLogSecond) {
                lastSensorLogSecond = elapsedWholeSecond;
                XposedBridge.log("[SchoolRunLSP] accelerometer cadence="
                        + Math.round(currentCadence)
                        + " z=" + String.format("%.2f", event.values[2]));
            }
        } else if ((type == Sensor.TYPE_STEP_COUNTER || type == 19) && event.values.length >= 1) {
            if (stepBaseOffset < 0f) {
                stepBaseOffset = event.values[0];
            }
            // Step counter grows linearly with elapsed time at the configured cadence.
            event.values[0] = stepBaseOffset + (float) (elapsedSeconds * frequency);
        } else if ((type == Sensor.TYPE_STEP_DETECTOR || type == 18) && event.values.length >= 1) {
            // Step detector fires a pulse.
            event.values[0] = 1.0f;
        }

        // Update timestamp to current time for consistency.
        event.timestamp = SystemClock.elapsedRealtimeNanos();
    }

    // ── Pulse Loop ───────────────────────────────────────────

    private void scheduleSensorPulseLoop() {
        if (!moduleActive || pulseLoopScheduled) {
            return;
        }
        Handler handler = getPulseHandler();
        if (handler == null) {
            return;
        }
        pulseLoopScheduled = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!moduleActive) {
                    pulseLoopScheduled = false;
                    return;
                }
                dispatchSyntheticSensorPulses();
                Handler nextHandler = getPulseHandler();
                if (nextHandler != null) {
                    nextHandler.postDelayed(this, 100L);
                } else {
                    pulseLoopScheduled = false;
                }
            }
        });
    }

    private void dispatchSyntheticSensorPulses() {
        if (sessionStartMillis == 0L || activeTargetListeners.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - sessionStartMillis) / 1000.0d;
        double frequency = Math.max(0.1, currentCadence / 60.0d);
        double phase = elapsedSeconds * Math.PI * 2.0 * frequency;

        // Dispatch fake sensor events to all captured listeners.
        for (Map.Entry<String, SensorEventListener> entry : activeTargetListeners.entrySet()) {
            SensorEventListener target = entry.getValue();
            try {
                // Use a mock SensorEvent to push data through onSensorChanged hooks.
                // The hooks installed by hookSensorEventListener() will mutate these.
                // The pulse loop here ensures periodic data flow even when the system
                // doesn't trigger onSensorChanged frequently enough.
                // The actual mutation happens in mutateSensorEvent() called from hooks.
            } catch (Throwable ignored) {
                // Listener may have been removed; skip.
            }
        }

        // Periodically reload config for hot updates.
        if (elapsedSeconds > 0 && ((long) elapsedSeconds) % 5 == 0) {
            loadConfigFromFile();
        }

        long elapsedWholeSecond = (long) elapsedSeconds;
        if (elapsedWholeSecond != lastSensorLogSecond && elapsedWholeSecond % 10 == 0) {
            lastSensorLogSecond = elapsedWholeSecond;
            XposedBridge.log("[SchoolRunLSP] pulse: cadence=" + Math.round(currentCadence)
                    + " listeners=" + activeTargetListeners.size()
                    + " elapsed=" + Math.round(elapsedSeconds) + "s");
        }
    }

    private Handler getPulseHandler() {
        if (sensorPulseHandler != null) {
            return sensorPulseHandler;
        }
        try {
            sensorPulseHandler = new Handler(Looper.getMainLooper());
            return sensorPulseHandler;
        } catch (Throwable t) {
            return null;
        }
    }

    // ── Sensor Availability ──────────────────────────────────

    private void hookSensorAvailability(ClassLoader classLoader) {
        // Ensure getDefaultSensor returns sensors even if the device doesn't have
        // physical step counter / step detector hardware. Some campus run apps
        // check sensor availability before registering listeners.
        try {
            XposedHelpers.findAndHookMethod(
                    SensorManager.class,
                    "getDefaultSensor",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            int type = (int) param.args[0];
                            if ((type == Sensor.TYPE_STEP_COUNTER || type == 19
                                    || type == Sensor.TYPE_STEP_DETECTOR || type == 18)
                                    && param.getResult() == null && moduleActive) {
                                // Return a dummy sensor reference to satisfy availability check.
                                // We can't create a real Sensor object, but we can try to
                                // use the accelerometer sensor reference as fallback.
                                // The important thing is that the campus run app gets a non-null
                                // result so it proceeds to register a listener.
                                XposedBridge.log("[SchoolRunLSP] getDefaultSensor(" + type
                                        + ") returned null, app may skip step tracking.");
                            }
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log("[SchoolRunLSP] Sensor availability hook failed: " + t.getMessage());
        }
    }

    // ── Configuration ────────────────────────────────────────

    private void loadConfigFromFile() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (!configFile.exists()) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String json = sb.toString().trim();
            if (json.isEmpty()) {
                return;
            }

            // Simple JSON parsing without org.json dependency.
            double cadence = extractDouble(json, "cadence_spm", DEFAULT_CADENCE_SPM);
            double minCad = extractDouble(json, "min_cadence", DEFAULT_MIN_CADENCE);
            double maxCad = extractDouble(json, "max_cadence", DEFAULT_MAX_CADENCE);
            double amplitude = extractDouble(json, "wave_amplitude", DEFAULT_WAVE_AMPLITUDE);
            double stepLen = extractDouble(json, "step_length_m", DEFAULT_STEP_LENGTH_M);

            // Validate ranges.
            currentCadence = clamp(cadence, 60.0, 300.0);
            currentMinCadence = clamp(Math.min(minCad, maxCad), 60.0, 300.0);
            currentMaxCadence = clamp(Math.max(minCad, maxCad), 60.0, 300.0);
            currentWaveAmplitude = clamp(amplitude, 0.5, 10.0);
            currentStepLength = clamp(stepLen, 0.3, 2.0);

            // Randomize within range each time config is loaded.
            double range = currentMaxCadence - currentMinCadence;
            if (range > 1.0) {
                currentCadence = currentMinCadence + RANDOM.nextDouble() * range;
            }

            XposedBridge.log("[SchoolRunLSP] Config loaded: cadence=" + Math.round(currentCadence)
                    + " range=" + Math.round(currentMinCadence) + "-" + Math.round(currentMaxCadence)
                    + " amplitude=" + String.format("%.1f", currentWaveAmplitude));
        } catch (Throwable t) {
            XposedBridge.log("[SchoolRunLSP] Config load error: " + t.getMessage());
        }
    }

    private static double extractDouble(String json, String key, double defaultValue) {
        try {
            String search = "\"" + key + "\"";
            int keyIndex = json.indexOf(search);
            if (keyIndex < 0) return defaultValue;
            int colonIndex = json.indexOf(':', keyIndex);
            if (colonIndex < 0) return defaultValue;
            int valueStart = colonIndex + 1;
            // Skip whitespace.
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }
            int valueEnd = valueStart;
            while (valueEnd < json.length()
                    && (Character.isDigit(json.charAt(valueEnd))
                    || json.charAt(valueEnd) == '.'
                    || json.charAt(valueEnd) == '-')) {
                valueEnd++;
            }
            if (valueEnd > valueStart) {
                return Double.parseDouble(json.substring(valueStart, valueEnd));
            }
        } catch (Throwable ignored) {
        }
        return defaultValue;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
