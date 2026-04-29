package com.acooldog.toolbox.root;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NfcAdapter;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public final class RootDiagnosticLsposedModule implements IXposedHookLoadPackage {
    private static final String MODULE_PACKAGE = "com.acooldog.toolbox";
    private static final Set<String> EXCLUDED_PACKAGES = new HashSet<>();
    private static final Set<String> hookedSensorListenerClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> hookedLocationListenerClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static volatile boolean active;
    private static volatile String activeSessionId = "";
    private static volatile Set<RootDiagnosticModule> activeModules =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile RootDiagnosticSettings activeSettings = RootDiagnosticSettings.defaults();
    private static volatile long sensorStartMillis;
    private static volatile float stepBaseOffset = -1f;
    private static volatile double currentCadence;
    private static volatile long lastSensorLogSecond = -1L;

    static {
        EXCLUDED_PACKAGES.add(MODULE_PACKAGE);
        EXCLUDED_PACKAGES.add("android");
        EXCLUDED_PACKAGES.add("com.android.systemui");
        EXCLUDED_PACKAGES.add("com.android.launcher");
        EXCLUDED_PACKAGES.add("com.android.settings");
        EXCLUDED_PACKAGES.add("com.android.packageinstaller");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        String packageName = lpparam == null ? "" : lpparam.packageName;
        if (packageName == null || EXCLUDED_PACKAGES.contains(packageName)) {
            return;
        }
        XposedBridge.log("SchoolRunDiag LSPosed loaded for package: " + packageName);
        installSessionReceiver(packageName);
        installLocationHooks(packageName);
        installSignalHooks(packageName);
        installDetectionBypassHooks(packageName);
        installServiceStreamHooks(packageName);
        installSensorHooks(packageName, lpparam.classLoader);
        logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "lsposed_loaded",
                "LSPosed模块已在作用域进程加载：" + packageName);
    }

    private void installSessionReceiver(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @SuppressLint("UnspecifiedRegisterReceiverFlag")
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Application application = (Application) param.thisObject;
                    BroadcastReceiver receiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            handleControlIntent(packageName, intent);
                        }
                    };
                    IntentFilter filter = new IntentFilter(LsposedDiagnosticBridge.ACTION_DIAGNOSTIC_CONTROL);
                    registerControlReceiver(application, receiver, filter);
                    logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "receiver_registered",
                            "LSPosed诊断控制广播已注册。");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag receiver hook failed: " + throwable.getMessage());
        }
    }

    private void registerControlReceiver(
            Application application,
            BroadcastReceiver receiver,
            IntentFilter filter
    ) {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                Method method = Context.class.getMethod(
                        "registerReceiver",
                        BroadcastReceiver.class,
                        IntentFilter.class,
                        int.class
                );
                method.invoke(application, receiver, filter, 2);
                return;
            } catch (Throwable ignored) {
                // Fall back to the legacy call for vendor builds that keep old behavior.
            }
        }
        application.registerReceiver(receiver, filter);
    }

    private void handleControlIntent(String packageName, Intent intent) {
        if (intent == null || !LsposedDiagnosticBridge.ACTION_DIAGNOSTIC_CONTROL.equals(intent.getAction())) {
            return;
        }
        String command = intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_COMMAND);
        if (LsposedDiagnosticBridge.COMMAND_START.equals(command)) {
            activeSessionId = safeString(intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SESSION_ID));
            List<RootDiagnosticModule> modules = LsposedDiagnosticBridge.parseModules(
                    intent.getStringArrayExtra(LsposedDiagnosticBridge.EXTRA_MODULE_IDS)
            );
            Set<RootDiagnosticModule> nextModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
            nextModules.addAll(modules);
            activeModules = nextModules;
            activeSettings = RootDiagnosticSettings.fromJson(
                    intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SETTINGS_JSON)
            );
            active = true;
            resetSensorState();
            logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "lsposed_session_started",
                    "LSPosed作用域诊断已启动，模块数=" + modules.size());
            for (RootDiagnosticModule module : modules) {
                logEvent(packageName, module.getId(), "module_enabled",
                        module.getTitle() + "；设置：" + activeSettings.summarize(module));
            }
            return;
        }
        if (LsposedDiagnosticBridge.COMMAND_UPDATE_LOCATION.equals(command)) {
            if (!active || !safeString(intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SESSION_ID)).equals(activeSessionId)) {
                return;
            }
            activeSettings = RootDiagnosticSettings.fromJson(
                    intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SETTINGS_JSON)
            );
            logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "route_location_updated",
                    "lat=" + activeSettings.getLocationLatitude()
                            + ", lon=" + activeSettings.getLocationLongitude()
                            + ", speed=" + activeSettings.getLocationSpeedMetersPerSecond());
            return;
        }
        if (LsposedDiagnosticBridge.COMMAND_STOP.equals(command)) {
            logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "lsposed_session_stopped",
                    "LSPosed作用域诊断已停止。");
            active = false;
            activeSessionId = "";
            activeModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
            resetSensorState();
        }
    }

    private void installLocationHooks(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(Location.class, "getLatitude", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult(activeSettings.getLocationLatitude());
                        logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "return_override",
                                "Location.getLatitude");
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Location.class, "getLongitude", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult(activeSettings.getLocationLongitude());
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Location.class, "getSpeed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult((float) activeSettings.getLocationSpeedMetersPerSecond());
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Location.class, "hasSpeed", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult(true);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Location.class, "getExtras", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    Bundle extras = param.getResult() instanceof Bundle ? (Bundle) param.getResult() : new Bundle();
                    extras.putInt("satellites", activeSettings.getLocationSatellites());
                    extras.putDouble("hdop", activeSettings.getLocationHdop());
                    param.setResult(extras);
                }
            });
            hookLocationManagerRegisterMethods(packageName, LocationManager.class);
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag location hooks failed: " + throwable.getMessage());
        }
    }

    private void hookLocationManagerRegisterMethods(String packageName, Class<?> managerClass) {
        for (Method method : managerClass.getDeclaredMethods()) {
            if (!"requestLocationUpdates".equals(method.getName())) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    for (Object arg : param.args) {
                        if (arg instanceof LocationListener) {
                            hookLocationListener(packageName, (LocationListener) arg);
                        }
                    }
                }
            });
        }
    }

    private void hookLocationListener(String packageName, LocationListener listener) {
        Class<?> listenerClass = listener.getClass();
        String className = listenerClass.getName();
        if (!hookedLocationListenerClasses.add(className)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(listenerClass, "onLocationChanged", Location.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    if (param.args.length > 0 && param.args[0] instanceof Location) {
                        Location location = (Location) param.args[0];
                        location.setLatitude(activeSettings.getLocationLatitude());
                        location.setLongitude(activeSettings.getLocationLongitude());
                        location.setSpeed((float) activeSettings.getLocationSpeedMetersPerSecond());
                        Bundle extras = location.getExtras();
                        if (extras == null) {
                            extras = new Bundle();
                        }
                        extras.putInt("satellites", activeSettings.getLocationSatellites());
                        extras.putDouble("hdop", activeSettings.getLocationHdop());
                        location.setExtras(extras);
                        logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "data_injected",
                                "LocationListener.onLocationChanged -> " + className);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag location listener hook failed: " + throwable.getMessage());
        }
    }

    private void installSignalHooks(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getBSSID", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "WifiInfo.getBSSID",
                    () -> activeSettings.getWifiBssid()
            ));
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getSSID", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "WifiInfo.getSSID",
                    () -> "\"" + activeSettings.getWifiSsid() + "\""
            ));
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkOperator", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "TelephonyManager.getNetworkOperator",
                    () -> activeSettings.getNetworkOperator()
            ));
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimOperator", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "TelephonyManager.getSimOperator",
                    () -> activeSettings.getNetworkOperator()
            ));
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkCountryIso", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "TelephonyManager.getNetworkCountryIso",
                    () -> activeSettings.getNetworkCountry()
            ));
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getSimCountryIso", returnStringHook(
                    packageName,
                    RootDiagnosticModule.RADIO_WIFI_SIGNAL,
                    "TelephonyManager.getSimCountryIso",
                    () -> activeSettings.getNetworkCountry()
            ));
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag signal hooks failed: " + throwable.getMessage());
        }
    }

    private void installDetectionBypassHooks(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(Debug.class, "isDebuggerConnected", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.DETECTION_BYPASS)
                            && activeSettings.isBypassDebugger()) {
                        param.setResult(false);
                        logEvent(packageName, RootDiagnosticModule.DETECTION_BYPASS, "return_override",
                                "Debug.isDebuggerConnected");
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Debug.class, "waitingForDebugger", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.DETECTION_BYPASS)
                            && activeSettings.isBypassDebugger()) {
                        param.setResult(false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(File.class, "exists", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.DETECTION_BYPASS)
                            || !activeSettings.isBypassRootArtifacts()) {
                        return;
                    }
                    File file = (File) param.thisObject;
                    String path = file == null ? "" : file.getAbsolutePath();
                    if (path.matches("(?i).*(/su$|/magisk|/busybox$|xposed|frida|zygisk).*")) {
                        param.setResult(false);
                        logEvent(packageName, RootDiagnosticModule.DETECTION_BYPASS, "return_override",
                                "File.exists " + path);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(Settings.Secure.class, "getString",
                    android.content.ContentResolver.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (isModuleActive(RootDiagnosticModule.DETECTION_BYPASS)
                                    && activeSettings.isBypassMockLocation()
                                    && "mock_location".equals(param.args[1])) {
                                param.setResult("0");
                                logEvent(packageName, RootDiagnosticModule.DETECTION_BYPASS, "return_override",
                                        "Settings.Secure.getString mock_location");
                            }
                        }
                    });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag detection hooks failed: " + throwable.getMessage());
        }
    }

    private void installServiceStreamHooks(String packageName) {
        try {
            Class<?> clipboard = XposedHelpers.findClass("android.content.ClipboardManager", null);
            XposedHelpers.findAndHookMethod(clipboard, "hasPrimaryClip", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.SERVICE_STREAM)
                            && activeSettings.isServiceClipboardNull()) {
                        param.setResult(false);
                        logEvent(packageName, RootDiagnosticModule.SERVICE_STREAM, "data_blocked",
                                "ClipboardManager.hasPrimaryClip");
                    }
                }
            });
            XposedHelpers.findAndHookMethod(clipboard, "getPrimaryClip", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.SERVICE_STREAM)
                            && activeSettings.isServiceClipboardNull()) {
                        param.setResult(null);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(BluetoothAdapter.class, "isEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.SERVICE_STREAM)
                            && activeSettings.isServiceBluetoothDisabled()) {
                        param.setResult(false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(NfcAdapter.class, "isEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.SERVICE_STREAM)
                            && activeSettings.isServiceNfcDisabled()) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag service hooks failed: " + throwable.getMessage());
        }
    }

    private void installSensorHooks(String packageName, ClassLoader classLoader) {
        try {
            hookRegisterListenerMethods(packageName, SensorManager.class);
            try {
                Class<?> systemSensorManager = XposedHelpers.findClass(
                        "android.hardware.SystemSensorManager",
                        classLoader
                );
                hookRegisterListenerMethods(packageName, systemSensorManager);
            } catch (Throwable ignored) {
                // Device implementation may not expose the class through this loader.
            }
            try {
                Class<?> oplusSensorManager = XposedHelpers.findClass(
                        "com.oplus.sensor.OplusSensorManager",
                        classLoader
                );
                hookRegisterListenerMethods(packageName, oplusSensorManager);
                logEvent(packageName, RootDiagnosticModule.SENSOR_INJECTION, "vendor_adapter_ready",
                        "ColorOS/OnePlus OplusSensorManager registerListener");
            } catch (Throwable ignored) {
                // Internal FUCK-RUN supports this vendor path when present; other devices do not expose it.
            }
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag sensor hooks failed: " + throwable.getMessage());
        }
    }

    private void hookRegisterListenerMethods(String packageName, Class<?> managerClass) {
        if (managerClass == null) {
            return;
        }
        for (Method method : managerClass.getDeclaredMethods()) {
            if (!"registerListener".equals(method.getName())) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.SENSOR_INJECTION)) {
                        return;
                    }
                    if (param.args.length > 0 && param.args[0] instanceof SensorEventListener) {
                        hookSensorEventListener(packageName, (SensorEventListener) param.args[0]);
                    }
                }
            });
        }
    }

    private void hookSensorEventListener(String packageName, SensorEventListener listener) {
        Class<?> listenerClass = listener.getClass();
        String className = listenerClass.getName();
        if (!hookedSensorListenerClasses.add(className)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(listenerClass, "onSensorChanged", SensorEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.SENSOR_INJECTION)) {
                        return;
                    }
                    SensorEvent event = (SensorEvent) param.args[0];
                    if (event == null || event.sensor == null || event.values == null) {
                        return;
                    }
                    int type = event.sensor.getType();
                    if (type != Sensor.TYPE_ACCELEROMETER
                            && type != Sensor.TYPE_STEP_COUNTER
                            && type != Sensor.TYPE_STEP_DETECTOR
                            && type != 19) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    if (sensorStartMillis == 0L) {
                        resetSensorState();
                    }
                    double elapsedSeconds = Math.max(0d, (now - sensorStartMillis) / 1000d);
                    double frequency = currentCadence / 60d;
                    double phase = elapsedSeconds * Math.PI * 2d * frequency;
                    if (type == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
                        double jitterY = (Math.random() * 0.3d) - 0.15d;
                        double jitterZ = (Math.random() * 0.3d) - 0.15d;
                        event.values[0] = (float) ((Math.random() * 0.1d) - 0.05d);
                        event.values[1] = (float) (Math.cos(phase) * 1.5d + jitterY);
                        event.values[2] = (float) (9.81d
                                + Math.sin(phase) * activeSettings.getSensorWaveAmplitude()
                                + jitterZ);
                        long elapsedWholeSecond = (long) elapsedSeconds;
                        if (elapsedWholeSecond != lastSensorLogSecond) {
                            lastSensorLogSecond = elapsedWholeSecond;
                            logEvent(packageName, RootDiagnosticModule.SENSOR_INJECTION, "data_injected",
                                    "accelerometer cadence=" + Math.round(currentCadence));
                        }
                    } else if ((type == Sensor.TYPE_STEP_COUNTER || type == 19) && event.values.length >= 1) {
                        if (stepBaseOffset < 0f) {
                            stepBaseOffset = event.values[0];
                        }
                        event.values[0] = stepBaseOffset + (float) (elapsedSeconds * frequency);
                    } else if (type == Sensor.TYPE_STEP_DETECTOR && event.values.length >= 1) {
                        event.values[0] = 1.0f;
                    }
                    event.timestamp = SystemClock.elapsedRealtimeNanos();
                }
            });
            logEvent(packageName, RootDiagnosticModule.SENSOR_INJECTION, "hook_installed",
                    "SensorEventListener.onSensorChanged -> " + className);
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag sensor listener hook failed: " + throwable.getMessage());
        }
    }

    private XC_MethodHook returnStringHook(
            String packageName,
            RootDiagnosticModule module,
            String surface,
            StringSupplier supplier
    ) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!isModuleActive(module)) {
                    return;
                }
                param.setResult(supplier.get());
                logEvent(packageName, module, "return_override", surface);
            }
        };
    }

    private boolean isModuleActive(RootDiagnosticModule module) {
        return active && activeModules.contains(module);
    }

    private static void resetSensorState() {
        sensorStartMillis = System.currentTimeMillis();
        stepBaseOffset = -1f;
        lastSensorLogSecond = -1L;
        RootDiagnosticSettings settings = activeSettings == null
                ? RootDiagnosticSettings.defaults()
                : activeSettings;
        double min = settings.getSensorMinCadence();
        double max = settings.getSensorMaxCadence();
        currentCadence = min + Math.random() * Math.max(0d, max - min);
    }

    private void logEvent(String packageName, RootDiagnosticModule module, String type, String detail) {
        logEvent(packageName, module.getId(), type, detail);
    }

    private void logEvent(String packageName, String moduleId, String type, String detail) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("sessionId", activeSessionId);
            payload.put("targetPackageName", packageName);
            payload.put("module", moduleId);
            payload.put("type", type);
            payload.put("detail", detail);
            XposedBridge.log(RootDiagnosticEvent.FRIDA_PREFIX + payload);
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag event log failed: " + throwable.getMessage());
        }
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private interface StringSupplier {
        String get();
    }
}
