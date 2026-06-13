package com.acooldog.toolbox.lspatch;

import android.content.Context;
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
import java.lang.reflect.Constructor;
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
 * 策略：
 * 1. Hook Application.onCreate — 捕获Context、缓存设备传感器引用、加载配置。
 * 2. Hook SensorManager.registerListener — 捕获计步/加速度监听器。
 * 3. Hook SensorEventListener.onSensorChanged — 篡改系统传来的SensorEvent。
 * 4. 脉冲循环(100ms) — 主动构造SensorEvent并通过反射调用onSensorChanged，
 *    确保即使设备无物理传感器，校园跑也能持续收到模拟数据。
 * 5. Hook SensorManager.getDefaultSensor — 当物理计步传感器缺失时返回加速度计，
 *    避免校园跑因检测不到传感器而拒绝启动。
 * <p>
 * 配置热更新: /sdcard/schoolrun_lspatch.json (每5秒重载)
 */
public final class LspatchStepModule implements IXposedHookLoadPackage {

    // ── 目标配置 ──────────────────────────────────────────────
    private static final String TARGET_PACKAGE = "com.eg.android.AlipayGphone";
    private static final String CONFIG_FILE_PATH = "/sdcard/schoolrun_lspatch.json";
    private static final String TAG = "[SchoolRunLSP]";

    // ── 默认传感器参数 ─────────────────────────────────────────
    private static final double DEFAULT_CADENCE_SPM = 180.0;
    private static final double DEFAULT_MIN_CADENCE = 140.0;
    private static final double DEFAULT_MAX_CADENCE = 220.0;
    private static final double DEFAULT_WAVE_AMPLITUDE = 2.5;
    private static final double DEFAULT_STEP_LENGTH_M = 0.8;
    private static final double JITTER_AMPLITUDE = 0.15;
    private static final long PULSE_INTERVAL_MS = 100L;
    private static final long CONFIG_RELOAD_INTERVAL_MS = 5000L;

    // ── 运行时状态 ─────────────────────────────────────────────
    private static volatile boolean moduleActive;
    private static volatile boolean pulseLoopScheduled;
    private static volatile long sessionStartMillis;
    private static volatile float stepBaseOffset = -1f;
    private static volatile long lastSensorLogSecond = -1L;
    private static volatile long lastConfigReloadMillis;

    // ── 可配置参数 (支持热更新) ────────────────────────────────
    private static volatile double currentCadence = DEFAULT_CADENCE_SPM;
    private static volatile double currentMinCadence = DEFAULT_MIN_CADENCE;
    private static volatile double currentMaxCadence = DEFAULT_MAX_CADENCE;
    private static volatile double currentWaveAmplitude = DEFAULT_WAVE_AMPLITUDE;
    private static volatile double currentStepLength = DEFAULT_STEP_LENGTH_M;

    private static final Random RANDOM = new Random();

    // ── 传感器注入基础设施 ─────────────────────────────────────
    /** 每种类型对应的真实Sensor对象，来自registerListener或启动缓存 */
    private static final Map<Integer, Sensor> sensorByType = new ConcurrentHashMap<>();
    /** 每个监听器对应的目标传感器类型 */
    private static final Map<SensorEventListener, Integer> listenerTargetType =
            new ConcurrentHashMap<>();
    /** 所有需要注入的活跃监听器 */
    private static final Set<SensorEventListener> activeTargetListeners =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** 已Hook的监听器类名，避免重复Hook */
    private static final Set<String> hookedSensorListenerClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    /** 已记录的错误日志去重 */
    private static final Set<String> loggedSensorErrors =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static Handler sensorPulseHandler;
    private static Context targetAppContext;
    /** SensorEvent(int) 构造器缓存（反射用） */
    private static Constructor<?> sensorEventConstructor;

    // ── getDefaultSensor 降级追踪 ────────────────────────────
    /**
     * 当 getDefaultSensor(TYPE_STEP_*) 返回 null 时，记录被请求的传感器类型，
     * 后续 registerListener 中据此修正 listenerTargetType。
     * 单线程调用（getDefaultSensor 和 registerListener 是顺序调用的），
     * 使用 volatile 足够。
     */
    private static volatile int pendingStepSensorSubstitution = -1;

    // ── IXposedHookLoadPackage ─────────────────────────────────

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || lpparam.packageName == null) return;
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) return;

        XposedBridge.log(TAG + " Loaded into: " + lpparam.processName);

        // Step 1: Hook Application.onCreate — 捕获Context、缓存传感器、加载配置
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
                            cacheDeviceSensors();
                            loadConfigFromFile();
                            moduleActive = true;
                            lastConfigReloadMillis = System.currentTimeMillis();
                            XposedBridge.log(TAG + " Activated. cadence="
                                    + Math.round(currentCadence) + " SPM range="
                                    + Math.round(currentMinCadence) + "-"
                                    + Math.round(currentMaxCadence));
                        }
                    });
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Application hook failed: " + t.getMessage());
        }

        // Step 2: Hook SensorManager.registerListener — 捕获目标监听器
        hookSensorManager(lpparam.classLoader);

        // Step 3: Hook SensorManager.getDefaultSensor — 计步传感器降级
        hookSensorAvailability(lpparam.classLoader);
    }

    // ── 缓存设备真实传感器 ─────────────────────────────────────

    private void cacheDeviceSensors() {
        try {
            SensorManager sm = (SensorManager) targetAppContext
                    .getSystemService(Context.SENSOR_SERVICE);
            if (sm == null) return;

            Sensor accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accel != null) {
                sensorByType.put(Sensor.TYPE_ACCELEROMETER, accel);
            }

            Sensor stepCounter = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepCounter != null) {
                sensorByType.put(Sensor.TYPE_STEP_COUNTER, stepCounter);
            }

            Sensor stepDetector = sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (stepDetector != null) {
                sensorByType.put(Sensor.TYPE_STEP_DETECTOR, stepDetector);
            }

            XposedBridge.log(TAG + " Cached sensors: accel=" + (accel != null)
                    + " stepCounter=" + (stepCounter != null)
                    + " stepDetector=" + (stepDetector != null));
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Cache sensors failed: " + t.getMessage());
        }
    }

    // ── SensorManager Hooks ──────────────────────────────────

    private void hookSensorManager(ClassLoader classLoader) {
        try {
            // registerListener(SensorEventListener, Sensor, int)
            XposedHelpers.findAndHookMethod(
                    SensorManager.class,
                    "registerListener",
                    SensorEventListener.class,
                    Sensor.class,
                    int.class,
                    new SensorRegisterHook());

            if (Build.VERSION.SDK_INT >= 19) {
                // registerListener(SensorEventListener, Sensor, int, int)
                XposedHelpers.findAndHookMethod(
                        SensorManager.class,
                        "registerListener",
                        SensorEventListener.class,
                        Sensor.class,
                        int.class,
                        int.class,
                        new SensorRegisterHook());

                // registerListener(SensorEventListener, Sensor, int, int, Handler)
                XposedHelpers.findAndHookMethod(
                        SensorManager.class,
                        "registerListener",
                        SensorEventListener.class,
                        Sensor.class,
                        int.class,
                        int.class,
                        Handler.class,
                        new SensorRegisterHook());
            }

            XposedBridge.log(TAG + " SensorManager hooks installed.");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " SensorManager hook failed: " + t.getMessage());
        }
    }

    private class SensorRegisterHook extends XC_MethodHook {
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            if (!moduleActive) return;
            SensorEventListener listener = (SensorEventListener) param.args[0];
            Sensor sensor = (Sensor) param.args[1];
            if (listener == null || sensor == null) return;
            captureSensorTarget(listener, sensor);
        }
    }

    /**
     * 捕获目标传感器监听器。
     * <p>
     * 如果 getDefaultSensor 之前做了降级（返回加速度计替代计步器），
     * 这里会检测到并修正 listenerTargetType 为实际请求的计步类型。
     */
    private void captureSensorTarget(SensorEventListener listener, Sensor sensor) {
        int actualType = sensor.getType();

        // 确定该监听器应该被注入的目标传感器类型
        int targetType;
        if (pendingStepSensorSubstitution >= 0
                && actualType == Sensor.TYPE_ACCELEROMETER) {
            // getDefaultSensor 降级：加速度计被当作计步传感器返回
            targetType = pendingStepSensorSubstitution;
            pendingStepSensorSubstitution = -1;
            XposedBridge.log(TAG + " Substituting sensor type: "
                    + actualType + " → " + targetType
                    + " for " + listener.getClass().getName());
        } else {
            targetType = actualType;
        }

        // 只处理加速度计、计步器、步检测器
        if (targetType != Sensor.TYPE_ACCELEROMETER
                && targetType != Sensor.TYPE_STEP_COUNTER
                && targetType != Sensor.TYPE_STEP_DETECTOR
                && targetType != 18
                && targetType != 19) {
            return;
        }

        // 缓存传感器引用（用真实类型）
        sensorByType.putIfAbsent(actualType, sensor);

        // 记录该监听器的目标类型
        activeTargetListeners.add(listener);
        listenerTargetType.put(listener, targetType);

        String listenerKey = listener.getClass().getName();
        if (hookedSensorListenerClasses.add(listenerKey)) {
            XposedBridge.log(TAG + " Captured: " + listenerKey + " targetType=" + targetType);
            hookSensorEventListener(listener);
        }

        // 启动脉冲循环
        scheduleSensorPulseLoop();
    }

    // ── Hook onSensorChanged ─────────────────────────────────

    /**
     * Hook 指定监听器的 onSensorChanged 方法。
     * 所有通过该监听器的 SensorEvent 都会先经过 mutateSensorEvent 篡改。
     */
    private void hookSensorEventListener(SensorEventListener listener) {
        try {
            XposedHelpers.findAndHookMethod(
                    listener.getClass(),
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
                XposedBridge.log(TAG + " Hook failed: " + listener.getClass().getName()
                        + " — " + t.getMessage());
            }
        }
    }

    // ── Sensor 数据生成 ──────────────────────────────────────

    private void ensureSessionStarted() {
        if (sessionStartMillis == 0L) {
            sessionStartMillis = System.currentTimeMillis();
            stepBaseOffset = -1f;
        }
    }

    /**
     * 篡改系统传来的 SensorEvent。
     * 同时作为脉冲循环的兜底——如果系统恰好在触发回调，这里再次确保数据正确。
     */
    private void mutateSensorEvent(SensorEvent event) {
        int type = event.sensor.getType();
        if (type != Sensor.TYPE_ACCELEROMETER
                && type != Sensor.TYPE_STEP_COUNTER
                && type != Sensor.TYPE_STEP_DETECTOR
                && type != 18 && type != 19) {
            return;
        }

        ensureSessionStarted();
        long now = System.currentTimeMillis();
        double elapsedSeconds = Math.max(0d, (now - sessionStartMillis) / 1000.0d);
        double frequency = currentCadence / 60.0d;
        double phase = elapsedSeconds * Math.PI * 2.0 * frequency;

        injectSensorData(event, type, phase, elapsedSeconds, frequency);
    }

    private void injectSensorData(SensorEvent event, int type, double phase,
                                   double elapsedSeconds, double frequency) {
        if (type == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
            double jitter = (RANDOM.nextDouble() * 2.0 - 1.0) * JITTER_AMPLITUDE;
            double wave = Math.sin(phase);
            double sideWave = Math.sin(phase + Math.PI * 0.5);
            event.values[0] = (float) ((RANDOM.nextDouble() * 2.0 - 1.0) * 0.2);
            event.values[1] = (float) (sideWave * 1.5 + jitter * 0.35);
            event.values[2] = (float) (9.81 + wave * currentWaveAmplitude + jitter);
        } else if ((type == Sensor.TYPE_STEP_COUNTER || type == 19)
                && event.values.length >= 1) {
            if (stepBaseOffset < 0f) {
                stepBaseOffset = event.values[0];
            }
            event.values[0] = stepBaseOffset + (float) (elapsedSeconds * frequency);
        } else if ((type == Sensor.TYPE_STEP_DETECTOR || type == 18)
                && event.values.length >= 1) {
            event.values[0] = 1.0f;
        }
        event.timestamp = SystemClock.elapsedRealtimeNanos();
    }

    // ── 脉冲循环 (核心) ──────────────────────────────────────

    private void scheduleSensorPulseLoop() {
        if (!moduleActive || pulseLoopScheduled) return;
        Handler handler = getPulseHandler();
        if (handler == null) return;
        pulseLoopScheduled = true;

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!moduleActive) {
                    pulseLoopScheduled = false;
                    return;
                }
                dispatchSyntheticSensorPulses();
                Handler h = getPulseHandler();
                if (h != null) {
                    h.postDelayed(this, PULSE_INTERVAL_MS);
                } else {
                    pulseLoopScheduled = false;
                }
            }
        });
    }

    /**
     * 主动构造 SensorEvent 并调用每个捕获的监听器的 onSensorChanged。
     * <p>
     * 这是确保非Root环境步频模拟能工作的核心：
     * 不依赖系统传感器触发回调，而是每100ms主动注入数据。
     * <p>
     * 对每个监听器根据其目标传感器类型分发对应数据：
     * - 加速度计: 正弦波(9.81±振幅)
     * - 计步器: 线性增长(base + t×频率)
     * - 步检测器: 脉冲1.0
     */
    private void dispatchSyntheticSensorPulses() {
        if (activeTargetListeners.isEmpty()) return;

        ensureSessionStarted();
        long now = System.currentTimeMillis();
        double elapsedSeconds = (now - sessionStartMillis) / 1000.0d;
        double frequency = Math.max(0.1, currentCadence / 60.0d);
        double phase = elapsedSeconds * Math.PI * 2.0 * frequency;

        // 定期热更新配置
        if (now - lastConfigReloadMillis >= CONFIG_RELOAD_INTERVAL_MS) {
            lastConfigReloadMillis = now;
            loadConfigFromFile();
        }

        // 对每个捕获的监听器，构造对应类型的 SensorEvent 并主动调用 onSensorChanged
        for (SensorEventListener listener : activeTargetListeners) {
            Integer targetType = listenerTargetType.get(listener);
            if (targetType == null) continue;

            // 获取对应的 Sensor 引用
            Sensor sensor = getSensorForDispatch(targetType);
            if (sensor == null) continue;

            try {
                SensorEvent event = buildSensorEvent(targetType, sensor, phase,
                        elapsedSeconds, frequency);
                if (event != null) {
                    // 直接调用 onSensorChanged — Hook 会在 beforeHookedMethod
                    // 中再次调用 mutateSensorEvent 作为兜底，确保数据一致。
                    listener.onSensorChanged(event);
                }
            } catch (Throwable ignored) {
                // 监听器可能已被GC或移除，忽略
            }
        }

        // 每10秒输出一次状态日志
        long elapsedWholeSecond = (long) elapsedSeconds;
        if (elapsedWholeSecond != lastSensorLogSecond && elapsedWholeSecond % 10 == 0) {
            lastSensorLogSecond = elapsedWholeSecond;
            XposedBridge.log(TAG + " pulse: cadence=" + Math.round(currentCadence)
                    + " listeners=" + activeTargetListeners.size()
                    + " elapsed=" + Math.round(elapsedSeconds) + "s"
                    + " sensors=" + sensorByType.size());
        }
    }

    /**
     * 获取用于分发的 Sensor 对象。
     * 优先使用 registerListener 中缓存的实际传感器；
     * 如果该类型缺失（例如设备无计步器），降级使用加速度计，
     * 因为数据注入发生在我们的 Hook 层，传感器类型已经不重要。
     */
    private Sensor getSensorForDispatch(int targetType) {
        // 优先匹配精确类型
        Sensor exact = sensorByType.get(targetType);
        if (exact != null) return exact;

        // 降级：使用加速度计（几乎所有设备都有）
        return sensorByType.get(Sensor.TYPE_ACCELEROMETER);
    }

    /**
     * 通过反射构造 SensorEvent。
     * <p>
     * SensorEvent 的构造器是 package-private: SensorEvent(int valueSize)。
     * 反射 setAccessible(true) 后调用。
     * <p>
     * 构造函数在各 Android 版本中稳定存在 (API 3+)。
     */
    private SensorEvent buildSensorEvent(int targetType, Sensor sensor, double phase,
                                          double elapsedSeconds, double frequency) {
        try {
            int valueCount = (targetType == Sensor.TYPE_ACCELEROMETER) ? 3 : 1;
            SensorEvent event = allocateSensorEvent(valueCount);
            if (event == null) return null;

            // 设置 Sensor 引用和元数据
            event.sensor = sensor;
            event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
            event.timestamp = SystemClock.elapsedRealtimeNanos();

            // 预填充传感器数据
            populateEventValues(event, targetType, phase, elapsedSeconds, frequency);

            return event;
        } catch (Throwable t) {
            String key = "buildEvent:" + t.getClass().getName();
            if (loggedSensorErrors.add(key)) {
                XposedBridge.log(TAG + " buildSensorEvent failed: " + t.getMessage());
            }
            return null;
        }
    }

    /**
     * 为 SensorEvent 填充 values 数组。
     */
    private void populateEventValues(SensorEvent event, int targetType, double phase,
                                      double elapsedSeconds, double frequency) {
        if (targetType == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
            double jitter = (RANDOM.nextDouble() * 2.0 - 1.0) * JITTER_AMPLITUDE;
            double wave = Math.sin(phase);
            double sideWave = Math.sin(phase + Math.PI * 0.5);
            event.values[0] = (float) ((RANDOM.nextDouble() * 2.0 - 1.0) * 0.2);
            event.values[1] = (float) (sideWave * 1.5 + jitter * 0.35);
            event.values[2] = (float) (9.81 + wave * currentWaveAmplitude + jitter);
        } else if ((targetType == Sensor.TYPE_STEP_COUNTER || targetType == 19)
                && event.values.length >= 1) {
            if (stepBaseOffset < 0f) stepBaseOffset = 0f;
            event.values[0] = stepBaseOffset + (float) (elapsedSeconds * frequency);
        } else if ((targetType == Sensor.TYPE_STEP_DETECTOR || targetType == 18)
                && event.values.length >= 1) {
            event.values[0] = 1.0f;
        }
    }

    /**
     * 通过反射创建 SensorEvent 实例。
     * SensorEvent(int) 构造器在 API 3+ 稳定存在。
     */
    private SensorEvent allocateSensorEvent(int valueCount) {
        try {
            if (sensorEventConstructor == null) {
                sensorEventConstructor = SensorEvent.class.getDeclaredConstructor(int.class);
                sensorEventConstructor.setAccessible(true);
            }
            return (SensorEvent) sensorEventConstructor.newInstance(valueCount);
        } catch (Throwable t) {
            String key = "allocEvent:" + t.getClass().getName();
            if (loggedSensorErrors.add(key)) {
                XposedBridge.log(TAG + " allocateSensorEvent(" + valueCount
                        + ") failed: " + t.getMessage());
            }
            return null;
        }
    }

    // ── getDefaultSensor 降级 ────────────────────────────────

    /**
     * Hook SensorManager.getDefaultSensor(int)。
     * 当设备缺少计步器/步检测器硬件时，返回加速度计作为降级，
     * 避免校园跑因 sensor == null 而跳过步频校验。
     * <p>
     * 降级信息记录在 {@link #pendingStepSensorSubstitution} 中，
     * 由后续的 captureSensorTarget 消费并修正 listenerTargetType。
     */
    private void hookSensorAvailability(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SensorManager.class,
                    "getDefaultSensor",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!moduleActive) return;
                            int type = (int) param.args[0];

                            boolean isStepSensor = (type == Sensor.TYPE_STEP_COUNTER
                                    || type == 19
                                    || type == Sensor.TYPE_STEP_DETECTOR
                                    || type == 18);

                            if (isStepSensor && param.getResult() == null) {
                                Sensor fallback = sensorByType.get(Sensor.TYPE_ACCELEROMETER);
                                if (fallback != null) {
                                    // 标记降级，让 captureSensorTarget 修正目标类型
                                    pendingStepSensorSubstitution = type;
                                    param.setResult(fallback);
                                    XposedBridge.log(TAG
                                            + " getDefaultSensor(" + type
                                            + ") → null, fallback to accelerometer");
                                } else {
                                    XposedBridge.log(TAG
                                            + " getDefaultSensor(" + type
                                            + ") → null, NO fallback available!");
                                }
                            }
                        }
                    });
            XposedBridge.log(TAG + " Sensor availability hook installed.");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Sensor availability hook failed: " + t.getMessage());
        }
    }

    private Handler getPulseHandler() {
        if (sensorPulseHandler != null) return sensorPulseHandler;
        try {
            sensorPulseHandler = new Handler(Looper.getMainLooper());
            return sensorPulseHandler;
        } catch (Throwable t) {
            return null;
        }
    }

    // ── 配置加载 ─────────────────────────────────────────────

    private void loadConfigFromFile() {
        try {
            File configFile = new File(CONFIG_FILE_PATH);
            if (!configFile.exists()) return;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String json = sb.toString().trim();
            if (json.isEmpty()) return;

            double cadence = extractDouble(json, "cadence_spm", DEFAULT_CADENCE_SPM);
            double minCad = extractDouble(json, "min_cadence", DEFAULT_MIN_CADENCE);
            double maxCad = extractDouble(json, "max_cadence", DEFAULT_MAX_CADENCE);
            double amplitude = extractDouble(json, "wave_amplitude", DEFAULT_WAVE_AMPLITUDE);
            double stepLen = extractDouble(json, "step_length_m", DEFAULT_STEP_LENGTH_M);

            currentCadence = clamp(cadence, 60.0, 300.0);
            currentMinCadence = clamp(Math.min(minCad, maxCad), 60.0, 300.0);
            currentMaxCadence = clamp(Math.max(minCad, maxCad), 60.0, 300.0);
            currentWaveAmplitude = clamp(amplitude, 0.5, 10.0);
            currentStepLength = clamp(stepLen, 0.3, 2.0);

            // 在范围内随机化当前步频
            double range = currentMaxCadence - currentMinCadence;
            if (range > 1.0) {
                currentCadence = currentMinCadence + RANDOM.nextDouble() * range;
            }

            XposedBridge.log(TAG + " Config loaded: cadence=" + Math.round(currentCadence)
                    + " range=" + Math.round(currentMinCadence) + "-"
                    + Math.round(currentMaxCadence)
                    + " amplitude=" + String.format("%.1f", currentWaveAmplitude));
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Config load error: " + t.getMessage());
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
            while (valueStart < json.length()
                    && Character.isWhitespace(json.charAt(valueStart))) {
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
