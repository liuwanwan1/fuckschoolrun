package com.acooldog.toolbox.root;

import android.annotation.SuppressLint;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.nfc.NfcAdapter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

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
    private static final Set<String> hookedPhoneStateListenerClasses =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Method> hookedRegisterMethods =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<Method> hookedSignalStrengthMethods =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<String> loggedSensorErrors =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ConcurrentHashMap<String, SensorInjectionTarget> sensorInjectionTargets =
            new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, LocationInjectionTarget> locationInjectionTargets =
            new ConcurrentHashMap<>();

    private static volatile boolean active;
    private static volatile String activeSessionId = "";
    private static volatile Set<RootDiagnosticModule> activeModules =
            Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static volatile RootDiagnosticSettings activeSettings = RootDiagnosticSettings.defaults();
    private static volatile long sensorStartMillis;
    private static volatile float stepBaseOffset = -1f;
    private static volatile double currentCadence;
    private static volatile long lastSensorLogSecond = -1L;
    private static volatile boolean sensorPulseScheduled;
    private static volatile boolean locationPulseScheduled;
    private static Handler sensorPulseHandler;
    private static Handler locationPulseHandler;

    static {
        EXCLUDED_PACKAGES.add(MODULE_PACKAGE);
        EXCLUDED_PACKAGES.add("android");
        EXCLUDED_PACKAGES.add("com.android.shell");
        EXCLUDED_PACKAGES.add("com.android.systemui");
        EXCLUDED_PACKAGES.add("com.android.launcher");
        EXCLUDED_PACKAGES.add("com.android.settings");
        EXCLUDED_PACKAGES.add("com.android.packageinstaller");
        EXCLUDED_PACKAGES.add("org.lsposed.manager");
        EXCLUDED_PACKAGES.add("de.robv.android.xposed.installer");
    }

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) {
        String packageName = lpparam == null ? "" : lpparam.packageName;
        if (packageName == null || shouldSkipPackage(packageName)) {
            return;
        }
        XposedBridge.log("SchoolRunDiag LSPosed loaded for package: " + packageName);
        installSessionReceiver(packageName);
        installLocationHooks(packageName);
        installSignalHooks(packageName);
        installSdkLocationHooks(packageName, lpparam.classLoader);
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
                    LsposedDiagnosticBridge.broadcastStateRequest(application, packageName);
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
            scheduleLocationPulseLoop(packageName);
            scheduleSensorPulseLoop(packageName);
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
            dispatchSyntheticLocationPulses(packageName);
            return;
        }
        if (LsposedDiagnosticBridge.COMMAND_UPDATE_SETTINGS.equals(command)) {
            if (!active || !safeString(intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SESSION_ID)).equals(activeSessionId)) {
                return;
            }
            activeSettings = RootDiagnosticSettings.fromJson(
                    intent.getStringExtra(LsposedDiagnosticBridge.EXTRA_SETTINGS_JSON)
            );
            if (activeModules.contains(RootDiagnosticModule.LOCATION_NMEA)) {
                scheduleLocationPulseLoop(packageName);
                dispatchSyntheticLocationPulses(packageName);
            }
            if (activeModules.contains(RootDiagnosticModule.SENSOR_INJECTION)) {
                resetSensorState();
                scheduleSensorPulseLoop(packageName);
            }
            logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "settings_updated",
                    "LSPosed作用域诊断设置已更新。");
            return;
        }
        if (LsposedDiagnosticBridge.COMMAND_STOP.equals(command)) {
            logEvent(packageName, RootDiagnosticEvent.MODULE_FRAMEWORK, "lsposed_session_stopped",
                    "LSPosed作用域诊断已停止。");
            active = false;
            activeSessionId = "";
            activeModules = Collections.newSetFromMap(new ConcurrentHashMap<>());
            resetSensorState();
            stopLocationPulseLoop();
            stopSensorPulseLoop();
        }
    }

    private void installLocationHooks(String packageName) {
        hookLocationConstructor(packageName);
        hookLocationCopy(packageName);
        hookLocationGetters(packageName);
        hookLocationManagerSurfaces(packageName);
    }

    private void installSdkLocationHooks(String packageName, ClassLoader classLoader) {
        hookGmsLocationResult(packageName, classLoader);
        hookOptionalNoArg(
                packageName,
                classLoader,
                "com.google.android.gms.location.LocationAvailability",
                "isLocationAvailable",
                RootDiagnosticModule.LOCATION_NMEA,
                () -> true
        );
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getLatitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLatitude());
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getLongitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLongitude());
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getAccuracy", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getSpeed", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) activeSettings.getLocationSpeedMetersPerSecond());
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getAltitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationAltitudeMeters());
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getBearing", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationBearingDegrees());
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getLocationType", RootDiagnosticModule.LOCATION_NMEA, () -> 1);
        hookOptionalNoArg(packageName, classLoader, "com.amap.api.location.AMapLocation",
                "getErrorCode", RootDiagnosticModule.LOCATION_NMEA, () -> 0);

        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getLatitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLatitude());
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getLongitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLongitude());
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getAccuracy", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getSpeed", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) activeSettings.getLocationSpeedMetersPerSecond());
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getAltitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationAltitudeMeters());
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getBearing", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationBearingDegrees());
        hookOptionalNoArg(packageName, classLoader, "com.tencent.map.geolocation.TencentLocation",
                "getProvider", RootDiagnosticModule.LOCATION_NMEA, () -> LocationManager.GPS_PROVIDER);

        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getLatitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLatitude());
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getLongitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationLongitude());
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getRadius", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getSpeed", RootDiagnosticModule.LOCATION_NMEA,
                () -> (float) activeSettings.getLocationSpeedMetersPerSecond());
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getAltitude", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationAltitudeMeters());
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getDirection", RootDiagnosticModule.LOCATION_NMEA, () -> activeSettings.getLocationBearingDegrees());
        hookOptionalNoArg(packageName, classLoader, "com.baidu.location.BDLocation",
                "getLocType", RootDiagnosticModule.LOCATION_NMEA, () -> 61);
    }

    private void hookGmsLocationResult(String packageName, ClassLoader classLoader) {
        try {
            Class<?> locationResultClass = XposedHelpers.findClass(
                    "com.google.android.gms.location.LocationResult",
                    classLoader
            );
            XposedBridge.hookAllMethods(locationResultClass, "getLastLocation", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    param.setResult(createSyntheticLocation("fused"));
                    logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "return_override",
                            "GMS LocationResult.getLastLocation");
                }
            });
            XposedBridge.hookAllMethods(locationResultClass, "getLocations", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    List<Location> locations = new ArrayList<>();
                    locations.add(createSyntheticLocation("fused"));
                    locations.add(createSyntheticLocation(LocationManager.GPS_PROVIDER));
                    param.setResult(locations);
                    logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "return_override",
                            "GMS LocationResult.getLocations");
                }
            });
            logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "hook_installed",
                    "GMS LocationResult");
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag GMS LocationResult hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationConstructor(String packageName) {
        try {
            XposedHelpers.findAndHookConstructor(Location.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        applySyntheticLocation(packageName, (Location) param.thisObject);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag Location constructor hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationCopy(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(Location.class, "set", Location.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        applySyntheticLocation(packageName, (Location) param.thisObject);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag Location.set hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationGetters(String packageName) {
        hookLocationGetter("getLatitude", activeSettings.getLocationLatitude());
        hookLocationGetter("getLongitude", activeSettings.getLocationLongitude());
        hookLocationGetter("getAccuracy", (float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
        hookLocationGetter("getSpeed", (float) activeSettings.getLocationSpeedMetersPerSecond());
        hookLocationGetter("getAltitude", activeSettings.getLocationAltitudeMeters());
        hookLocationGetter("getBearing", activeSettings.getLocationBearingDegrees());
        hookLocationBooleanGetter("hasSpeed");
        hookLocationBooleanGetter("hasAccuracy");
        hookLocationBooleanGetter("hasAltitude");
        hookLocationBooleanGetter("hasBearing");
        hookLocationBooleanGetter("isFromMockProvider", false);
        try {
            XposedHelpers.findAndHookMethod(Location.class, "getExtras", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    Bundle extras = param.getResult() instanceof Bundle ? (Bundle) param.getResult() : new Bundle();
                    fillLocationExtras(extras);
                    param.setResult(extras);
                    logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "return_override",
                            "Location.getExtras");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag Location.getExtras hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationGetter(String methodName, Object ignoredDefaultValue) {
        try {
            XposedHelpers.findAndHookMethod(Location.class, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    switch (methodName) {
                        case "getLatitude":
                            param.setResult(activeSettings.getLocationLatitude());
                            break;
                        case "getLongitude":
                            param.setResult(activeSettings.getLocationLongitude());
                            break;
                        case "getAccuracy":
                            param.setResult((float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
                            break;
                        case "getSpeed":
                            param.setResult((float) activeSettings.getLocationSpeedMetersPerSecond());
                            break;
                        case "getAltitude":
                            param.setResult(activeSettings.getLocationAltitudeMeters());
                            break;
                        case "getBearing":
                            param.setResult(activeSettings.getLocationBearingDegrees());
                            break;
                        default:
                            break;
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag Location." + methodName + " hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationBooleanGetter(String methodName) {
        hookLocationBooleanGetter(methodName, true);
    }

    private void hookLocationBooleanGetter(String methodName, boolean value) {
        try {
            XposedHelpers.findAndHookMethod(Location.class, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult(value);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag Location." + methodName + " hook skipped: " + throwable.getMessage());
        }
    }

    private void hookLocationManagerSurfaces(String packageName) {
        try {
            XposedBridge.hookAllMethods(LocationManager.class, "getLastKnownLocation", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        return;
                    }
                    Location location = param.getResult() instanceof Location ? (Location) param.getResult() : null;
                    if (location == null) {
                        String provider = param.args != null && param.args.length > 0 && param.args[0] instanceof String
                                ? (String) param.args[0]
                                : LocationManager.GPS_PROVIDER;
                        location = createSyntheticLocation(provider);
                    } else {
                        applySyntheticLocation(packageName, location);
                    }
                    param.setResult(location);
                    logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "return_override",
                            "LocationManager.getLastKnownLocation");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag getLastKnownLocation hook skipped: " + throwable.getMessage());
        }
        try {
            XposedBridge.hookAllMethods(LocationManager.class, "isProviderEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)
                            || param.args == null
                            || param.args.length == 0
                            || !(param.args[0] instanceof String)) {
                        return;
                    }
                    String provider = (String) param.args[0];
                    if (LocationManager.GPS_PROVIDER.equals(provider)
                            || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag isProviderEnabled hook skipped: " + throwable.getMessage());
        }
        try {
            XposedBridge.hookAllMethods(LocationManager.class, "addNmeaListener", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                        param.setResult(true);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag addNmeaListener hook skipped: " + throwable.getMessage());
        }
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                XposedHelpers.findAndHookMethod(GnssStatus.class, "getSatelliteCount", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                            param.setResult(activeSettings.getLocationSatellites());
                        }
                    }
                });
            }
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag GnssStatus hook skipped: " + throwable.getMessage());
        }
        hookLocationManagerRegisterMethods(packageName, LocationManager.class);
    }

    private void hookLocationManagerRegisterMethods(String packageName, Class<?> managerClass) {
        for (Method method : managerClass.getDeclaredMethods()) {
            if (!"requestLocationUpdates".equals(method.getName())) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        for (Object arg : param.args) {
                            if (arg instanceof LocationListener) {
                                LocationListener listener = (LocationListener) arg;
                                hookLocationListener(packageName, listener);
                                registerLocationInjectionTarget(listener);
                                if (isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                                    dispatchSyntheticLocation(packageName, listener);
                                    scheduleLocationPulseLoop(packageName);
                                }
                            }
                        }
                    }
                });
            } catch (Throwable throwable) {
                XposedBridge.log("SchoolRunDiag requestLocationUpdates overload skipped: "
                        + method + " " + throwable.getMessage());
            }
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
                        applySyntheticLocation(packageName, location);
                        logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "data_injected",
                                "LocationListener.onLocationChanged -> " + className);
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag location listener hook failed: " + throwable.getMessage());
        }
    }

    private void registerLocationInjectionTarget(LocationListener listener) {
        if (listener == null) {
            return;
        }
        String key = String.valueOf(System.identityHashCode(listener));
        locationInjectionTargets.put(key, new LocationInjectionTarget(listener));
    }

    private void scheduleLocationPulseLoop(String packageName) {
        if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)
                || locationInjectionTargets.isEmpty()
                || locationPulseScheduled) {
            return;
        }
        Handler handler = getLocationPulseHandler();
        if (handler == null) {
            return;
        }
        locationPulseScheduled = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
                    locationPulseScheduled = false;
                    return;
                }
                dispatchSyntheticLocationPulses(packageName);
                Handler nextHandler = getLocationPulseHandler();
                if (nextHandler != null) {
                    nextHandler.postDelayed(this, 500L);
                } else {
                    locationPulseScheduled = false;
                }
            }
        });
    }

    private void stopLocationPulseLoop() {
        locationPulseScheduled = false;
        Handler handler = locationPulseHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private Handler getLocationPulseHandler() {
        if (locationPulseHandler != null) {
            return locationPulseHandler;
        }
        try {
            locationPulseHandler = new Handler(Looper.getMainLooper());
            return locationPulseHandler;
        } catch (Throwable throwable) {
            return null;
        }
    }

    private void dispatchSyntheticLocationPulses(String packageName) {
        if (!isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
            return;
        }
        for (LocationInjectionTarget target : locationInjectionTargets.values()) {
            dispatchSyntheticLocation(packageName, target.listener);
        }
    }

    private void dispatchSyntheticLocation(String packageName, LocationListener listener) {
        Handler handler = getLocationPulseHandler();
        if (handler == null || listener == null) {
            return;
        }
        handler.post(() -> {
            try {
                listener.onLocationChanged(createSyntheticLocation(LocationManager.GPS_PROVIDER));
            } catch (Throwable throwable) {
                XposedBridge.log("SchoolRunDiag synthetic location pulse skipped: " + throwable.getMessage());
                logEvent(packageName, RootDiagnosticModule.LOCATION_NMEA, "synthetic_location_skipped",
                        throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
            }
        });
    }

    private Location createSyntheticLocation(String provider) {
        Location location = new Location(provider == null || provider.trim().isEmpty()
                ? LocationManager.GPS_PROVIDER
                : provider);
        applySyntheticLocation("", location);
        return location;
    }

    private void applySyntheticLocation(String packageName, Location location) {
        if (location == null || !isModuleActive(RootDiagnosticModule.LOCATION_NMEA)) {
            return;
        }
        try {
            location.setLatitude(activeSettings.getLocationLatitude());
            location.setLongitude(activeSettings.getLocationLongitude());
            location.setAccuracy((float) Math.max(3d, activeSettings.getLocationHdop() * 2.5d));
            location.setSpeed((float) activeSettings.getLocationSpeedMetersPerSecond());
            location.setAltitude(activeSettings.getLocationAltitudeMeters());
            location.setBearing(activeSettings.getLocationBearingDegrees());
            location.setTime(System.currentTimeMillis());
            if (Build.VERSION.SDK_INT >= 17) {
                location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
            }
            Bundle extras = new Bundle();
            fillLocationExtras(extras);
            location.setExtras(extras);
            try {
                XposedHelpers.setBooleanField(location, "mIsFromMockProvider", false);
            } catch (Throwable ignored) {
                // Android vendors keep this field private or rename it; getter hook still returns false.
            }
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag applySyntheticLocation failed: " + throwable.getMessage());
        }
    }

    private void fillLocationExtras(Bundle extras) {
        extras.putInt("satellites", activeSettings.getLocationSatellites());
        extras.putInt("satelliteCount", activeSettings.getLocationSatellites());
        extras.putDouble("hdop", activeSettings.getLocationHdop());
        extras.putString("source", LocationManager.GPS_PROVIDER);
        extras.putBoolean("mockLocation", false);
    }

    private void installSignalHooks(String packageName) {
        hookWifiSurfaces(packageName);
        hookRadioSurfaces(packageName);
        hookNetworkSurfaces(packageName);
    }

    private void hookWifiSurfaces(String packageName) {
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
            XposedHelpers.findAndHookMethod(WifiInfo.class, "getRssi", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        param.setResult(currentWifiRssiDbm());
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag WifiInfo getter hooks skipped: " + throwable.getMessage());
        }
        try {
            XposedBridge.hookAllMethods(WifiManager.class, "calculateSignalLevel", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        param.setResult(wifiSignalLevelForArgs(param.args));
                    }
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag WifiManager.calculateSignalLevel hook skipped: " + throwable.getMessage());
        }
        try {
            XposedHelpers.findAndHookMethod(WifiManager.class, "getConnectionInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)
                            || !(param.getResult() instanceof WifiInfo)) {
                        return;
                    }
                    WifiInfo info = (WifiInfo) param.getResult();
                    setObjectFieldQuietly(info, "mBSSID", activeSettings.getWifiBssid());
                    setObjectFieldQuietly(info, "mSSID", "\"" + activeSettings.getWifiSsid() + "\"");
                    setObjectFieldQuietly(info, "mMacAddress", "02:00:00:00:00:00");
                    setIntFieldQuietly(info, "mRssi", currentWifiRssiDbm());
                    logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "data_injected",
                            "WifiManager.getConnectionInfo");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag WifiManager.getConnectionInfo hook skipped: " + throwable.getMessage());
        }
        try {
            XposedHelpers.findAndHookMethod(WifiManager.class, "getScanResults", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        return;
                    }
                    param.setResult(createWifiScanResults());
                    logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "return_override",
                            "WifiManager.getScanResults");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag WifiManager.getScanResults hook skipped: " + throwable.getMessage());
        }
    }

    private void hookRadioSurfaces(String packageName) {
        hookSignalStrengthSurfaces(packageName);
        hookCellSignalStrengthSurfaces(packageName);
        try {
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
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getNetworkType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        param.setResult(TelephonyManager.NETWORK_TYPE_UNKNOWN);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getAllCellInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        return;
                    }
                    if (!(param.getResult() instanceof List)
                            || ((List<?>) param.getResult()).isEmpty()) {
                        param.setResult(createSyntheticCellInfoList());
                    }
                    logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "data_injected",
                            "TelephonyManager.getAllCellInfo signal getters");
                }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "getCellLocation", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                        param.setResult(null);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(TelephonyManager.class, "listen", PhoneStateListener.class, int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)
                                    && param.args != null
                                    && param.args.length > 0
                                    && param.args[0] instanceof PhoneStateListener) {
                                hookPhoneStateListener(packageName, (PhoneStateListener) param.args[0]);
                            }
                        }
                    });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag signal hooks failed: " + throwable.getMessage());
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                XposedHelpers.findAndHookMethod(TelephonyManager.class, "requestCellInfoUpdate",
                        Executor.class, TelephonyManager.CellInfoCallback.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                if (!isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)
                                        || param.args == null
                                        || param.args.length < 2) {
                                    return;
                                }
                                logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "api_call",
                                        "TelephonyManager.requestCellInfoUpdate pass-through with signal overrides");
                            }
                        });
            }
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag requestCellInfoUpdate hook skipped: " + throwable.getMessage());
        }
        try {
            XposedHelpers.findAndHookMethod(SubscriptionManager.class, "getActiveSubscriptionInfoList",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                                param.setResult(new ArrayList<SubscriptionInfo>());
                            }
                        }
                    });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag SubscriptionManager hook skipped: " + throwable.getMessage());
        }
    }

    private void hookSignalStrengthSurfaces(String packageName) {
        hookNoArgIntMethod(SignalStrength.class, "getLevel", () -> activeSettings.getSignalStrengthProfile().getCellLevel());
        hookNoArgIntMethod(SignalStrength.class, "getDbm", this::currentCellDbm);
        hookNoArgIntMethod(SignalStrength.class, "getAsuLevel", () -> activeSettings.getSignalStrengthProfile().getCellAsuLevel());
        hookNoArgIntMethod(SignalStrength.class, "getGsmSignalStrength", () -> activeSettings.getSignalStrengthProfile().getCellAsuLevel());
        hookNoArgIntMethod(SignalStrength.class, "getCdmaDbm", this::currentCellDbm);
        hookNoArgIntMethod(SignalStrength.class, "getEvdoDbm", this::currentCellDbm);
        hookNoArgIntMethod(SignalStrength.class, "getLteDbm", this::currentCellDbm);
        hookNoArgIntMethod(SignalStrength.class, "getLteRsrp", this::currentCellDbm);
        logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "hook_installed",
                "SignalStrength level/dbm/asu");
    }

    private void hookCellSignalStrengthSurfaces(String packageName) {
        String[] classNames = new String[] {
                "android.telephony.CellSignalStrength",
                "android.telephony.CellSignalStrengthGsm",
                "android.telephony.CellSignalStrengthCdma",
                "android.telephony.CellSignalStrengthLte",
                "android.telephony.CellSignalStrengthWcdma",
                "android.telephony.CellSignalStrengthTdscdma",
                "android.telephony.CellSignalStrengthNr"
        };
        for (String className : classNames) {
            try {
                Class<?> signalClass = XposedHelpers.findClass(className, null);
                hookNoArgIntMethod(signalClass, "getDbm", this::currentCellDbm);
                hookNoArgIntMethod(signalClass, "getAsuLevel", () -> activeSettings.getSignalStrengthProfile().getCellAsuLevel());
                hookNoArgIntMethod(signalClass, "getLevel", () -> activeSettings.getSignalStrengthProfile().getCellLevel());
                hookNoArgIntMethod(signalClass, "getRssi", this::currentCellDbm);
                hookNoArgIntMethod(signalClass, "getRsrp", this::currentCellDbm);
                hookNoArgIntMethod(signalClass, "getRscp", this::currentCellDbm);
                logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "hook_installed",
                        className + " strength getters");
            } catch (Throwable throwable) {
                XposedBridge.log("SchoolRunDiag CellSignalStrength hook skipped: "
                        + className + " " + throwable.getMessage());
            }
        }
    }

    private void hookNoArgIntMethod(Class<?> targetClass, String methodName, IntSupplier supplier) {
        if (targetClass == null) {
            return;
        }
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!methodName.equals(method.getName())
                    || method.getParameterTypes().length != 0
                    || method.getReturnType() != int.class
                    || !hookedSignalStrengthMethods.add(method)) {
                continue;
            }
            try {
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                            param.setResult(supplier.get());
                        }
                    }
                });
            } catch (Throwable throwable) {
                XposedBridge.log("SchoolRunDiag int hook skipped: "
                        + targetClass.getName() + "." + methodName + " " + throwable.getMessage());
            }
        }
    }

    private void hookPhoneStateListener(String packageName, PhoneStateListener listener) {
        Class<?> listenerClass = listener.getClass();
        String className = listenerClass.getName();
        if (!hookedPhoneStateListenerClasses.add(className)) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(listenerClass, "onSignalStrengthsChanged", SignalStrength.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
                                logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "data_injected",
                                        "PhoneStateListener.onSignalStrengthsChanged -> " + className);
                            }
                        }
                    });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag PhoneStateListener signal hook skipped: " + throwable.getMessage());
        }
    }

    private void hookNetworkSurfaces(String packageName) {
        try {
            XposedHelpers.findAndHookMethod(ConnectivityManager.class, "getActiveNetworkInfo", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(RootDiagnosticModule.RADIO_WIFI_SIGNAL)
                            || !(param.getResult() instanceof NetworkInfo)) {
                        return;
                    }
                    NetworkInfo info = (NetworkInfo) param.getResult();
                    setIntFieldQuietly(info, "mNetworkType", ConnectivityManager.TYPE_WIFI);
                    setObjectFieldQuietly(info, "mTypeName", "WIFI");
                    setObjectFieldQuietly(info, "mState", NetworkInfo.State.CONNECTED);
                    logEvent(packageName, RootDiagnosticModule.RADIO_WIFI_SIGNAL, "data_injected",
                            "ConnectivityManager.getActiveNetworkInfo");
                }
            });
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag ConnectivityManager hook skipped: " + throwable.getMessage());
        }
    }

    private List<ScanResult> createWifiScanResults() {
        List<ScanResult> results = new ArrayList<>();
        try {
            Constructor<ScanResult> constructor = ScanResult.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            ScanResult result = constructor.newInstance();
            result.SSID = activeSettings.getWifiSsid();
            result.BSSID = activeSettings.getWifiBssid();
            result.level = currentWifiRssiDbm();
            result.capabilities = "[WPA2-PSK-CCMP][ESS]";
            result.frequency = 2412;
            if (Build.VERSION.SDK_INT >= 17) {
                result.timestamp = SystemClock.elapsedRealtime() * 1000L;
            }
            results.add(result);
        } catch (Throwable ignored) {
            // Some vendor frameworks block ScanResult construction; return an empty controlled list.
        }
        return results;
    }

    private List<CellInfo> createSyntheticCellInfoList() {
        List<CellInfo> results = new ArrayList<>();
        try {
            CellInfoLte cellInfo = (CellInfoLte) XposedHelpers.newInstance(CellInfoLte.class);
            CellSignalStrengthLte strength =
                    (CellSignalStrengthLte) XposedHelpers.newInstance(CellSignalStrengthLte.class);
            int dbm = currentCellDbm();
            setIntFieldQuietly(strength, "mRssi", dbm);
            setIntFieldQuietly(strength, "mRsrp", dbm);
            setIntFieldQuietly(strength, "mLevel", activeSettings.getSignalStrengthProfile().getCellLevel());
            setObjectFieldQuietly(cellInfo, "mCellSignalStrengthLte", strength);
            results.add(cellInfo);
        } catch (Throwable ignored) {
            // Vendor framework layouts differ; signal getter hooks still cover real CellInfo instances.
        }
        return results;
    }

    private int currentWifiRssiDbm() {
        return jitteredDbm(activeSettings.getWifiRssiDbm(), activeSettings.getWifiJitterDbm(), -100, -30);
    }

    private int currentCellDbm() {
        return jitteredDbm(activeSettings.getCellDbm(), activeSettings.getCellJitterDbm(), -125, -50);
    }

    private int wifiSignalLevelForArgs(Object[] args) {
        int fiveLevelValue = activeSettings.getSignalStrengthProfile().getWifiLevel();
        int levels = 5;
        if (args != null && args.length > 1 && args[1] instanceof Integer) {
            levels = Math.max(1, (Integer) args[1]);
        }
        if (levels <= 1) {
            return 0;
        }
        return Math.min(levels - 1, Math.round(fiveLevelValue * (levels - 1) / 4f));
    }

    private int jitteredDbm(int baseDbm, int jitterDbm, int minDbm, int maxDbm) {
        if (jitterDbm <= 0) {
            return clamp(baseDbm, minDbm, maxDbm);
        }
        int spread = jitterDbm * 2 + 1;
        int delta = (int) (Math.random() * spread) - jitterDbm;
        return clamp(baseDbm + delta, minDbm, maxDbm);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void setObjectFieldQuietly(Object instance, String fieldName, Object value) {
        try {
            XposedHelpers.setObjectField(instance, fieldName, value);
        } catch (Throwable ignored) {
            // Field layout differs across Android releases and vendors.
        }
    }

    private void setIntFieldQuietly(Object instance, String fieldName, int value) {
        try {
            XposedHelpers.setIntField(instance, fieldName, value);
        } catch (Throwable ignored) {
            // Field layout differs across Android releases and vendors.
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
            if (!hookedRegisterMethods.add(method)) {
                continue;
            }
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length > 0 && param.args[0] instanceof SensorEventListener) {
                        SensorEventListener listener = (SensorEventListener) param.args[0];
                        hookSensorEventListener(packageName, listener);
                        Sensor sensor = findSensorArg(param.args);
                        if (sensor != null) {
                            registerSensorInjectionTarget(listener, sensor, findHandlerArg(param.args));
                            scheduleSensorPulseLoop(packageName);
                        }
                    }
                }
            });
        }
    }

    private Sensor findSensorArg(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Sensor) {
                return (Sensor) arg;
            }
        }
        return null;
    }

    private Handler findHandlerArg(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Handler) {
                return (Handler) arg;
            }
        }
        return null;
    }

    private void registerSensorInjectionTarget(
            SensorEventListener listener,
            Sensor sensor,
            Handler handler
    ) {
        int type = sensor.getType();
        if (type != Sensor.TYPE_ACCELEROMETER
                && type != Sensor.TYPE_STEP_COUNTER
                && type != Sensor.TYPE_STEP_DETECTOR
                && type != 19) {
            return;
        }
        String key = System.identityHashCode(listener) + ":" + type;
        sensorInjectionTargets.put(key, new SensorInjectionTarget(listener, sensor, handler));
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
                    safelyInjectSensorEvent(packageName, className, param);
                }
            });
            logEvent(packageName, RootDiagnosticModule.SENSOR_INJECTION, "hook_installed",
                    "SensorEventListener.onSensorChanged -> " + className);
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag sensor listener hook failed: " + throwable.getMessage());
        }
    }

    private void safelyInjectSensorEvent(
            String packageName,
            String listenerClassName,
            XC_MethodHook.MethodHookParam param
    ) {
        try {
            if (!isModuleActive(RootDiagnosticModule.SENSOR_INJECTION)) {
                return;
            }
            if (param.args == null || param.args.length == 0 || !(param.args[0] instanceof SensorEvent)) {
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
        } catch (Throwable throwable) {
            String key = listenerClassName + ":" + throwable.getClass().getName();
            if (loggedSensorErrors.add(key)) {
                XposedBridge.log("SchoolRunDiag sensor injection skipped for "
                        + listenerClassName + ": " + throwable.getMessage());
            }
        }
    }

    private void scheduleSensorPulseLoop(String packageName) {
        if (!isModuleActive(RootDiagnosticModule.SENSOR_INJECTION)
                || sensorInjectionTargets.isEmpty()
                || sensorPulseScheduled) {
            return;
        }
        Handler handler = getSensorPulseHandler();
        if (handler == null) {
            return;
        }
        sensorPulseScheduled = true;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isModuleActive(RootDiagnosticModule.SENSOR_INJECTION)) {
                    sensorPulseScheduled = false;
                    return;
                }
                dispatchSyntheticSensorPulses(packageName);
                Handler nextHandler = getSensorPulseHandler();
                if (nextHandler != null) {
                    nextHandler.postDelayed(this, 100L);
                } else {
                    sensorPulseScheduled = false;
                }
            }
        });
    }

    private void stopSensorPulseLoop() {
        sensorPulseScheduled = false;
        Handler handler = sensorPulseHandler;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private Handler getSensorPulseHandler() {
        if (sensorPulseHandler != null) {
            return sensorPulseHandler;
        }
        try {
            sensorPulseHandler = new Handler(Looper.getMainLooper());
            return sensorPulseHandler;
        } catch (Throwable throwable) {
            return null;
        }
    }

    private void dispatchSyntheticSensorPulses(String packageName) {
        long now = System.currentTimeMillis();
        if (sensorStartMillis == 0L) {
            resetSensorState();
        }
        double elapsedSeconds = Math.max(0d, (now - sensorStartMillis) / 1000d);
        double frequency = Math.max(0.1d, currentCadence / 60d);
        long detectorIntervalMillis = Math.max(250L, Math.round(1000d / frequency));
        for (SensorInjectionTarget target : sensorInjectionTargets.values()) {
            int type = target.sensor.getType();
            if (type == Sensor.TYPE_STEP_DETECTOR && now - target.lastDetectorPulseMillis < detectorIntervalMillis) {
                continue;
            }
            if (type == Sensor.TYPE_STEP_DETECTOR) {
                target.lastDetectorPulseMillis = now;
            }
            dispatchSyntheticSensorEvent(packageName, target, elapsedSeconds);
        }
    }

    private void dispatchSyntheticSensorEvent(
            String packageName,
            SensorInjectionTarget target,
            double elapsedSeconds
    ) {
        Handler targetHandler = target.handler == null ? getSensorPulseHandler() : target.handler;
        if (targetHandler == null) {
            return;
        }
        targetHandler.post(() -> {
            try {
                int type = target.sensor.getType();
                SensorEvent event = (SensorEvent) XposedHelpers.newInstance(
                        SensorEvent.class,
                        type == Sensor.TYPE_ACCELEROMETER ? 3 : 1
                );
                event.sensor = target.sensor;
                event.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
                event.timestamp = SystemClock.elapsedRealtimeNanos();
                if (type == Sensor.TYPE_ACCELEROMETER && event.values.length >= 3) {
                    double frequency = Math.max(0.1d, currentCadence / 60d);
                    double phase = elapsedSeconds * Math.PI * 2d * frequency;
                    event.values[0] = (float) ((Math.random() * 0.1d) - 0.05d);
                    event.values[1] = (float) (Math.cos(phase) * 1.5d + ((Math.random() * 0.3d) - 0.15d));
                    event.values[2] = (float) (9.81d
                            + Math.sin(phase) * activeSettings.getSensorWaveAmplitude()
                            + ((Math.random() * 0.3d) - 0.15d));
                } else if (type == Sensor.TYPE_STEP_COUNTER || type == 19) {
                    if (stepBaseOffset < 0f) {
                        stepBaseOffset = 0f;
                    }
                    event.values[0] = stepBaseOffset + (float) (elapsedSeconds * (currentCadence / 60d));
                } else if (type == Sensor.TYPE_STEP_DETECTOR) {
                    event.values[0] = 1.0f;
                } else {
                    return;
                }
                target.listener.onSensorChanged(event);
            } catch (Throwable throwable) {
                String key = "synthetic:" + target.sensor.getType() + ":" + throwable.getClass().getName();
                if (loggedSensorErrors.add(key)) {
                    XposedBridge.log("SchoolRunDiag synthetic sensor pulse skipped: " + throwable.getMessage());
                    logEvent(packageName, RootDiagnosticModule.SENSOR_INJECTION, "synthetic_pulse_skipped",
                            throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
                }
            }
        });
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

    private void hookOptionalNoArg(
            String packageName,
            ClassLoader classLoader,
            String className,
            String methodName,
            RootDiagnosticModule module,
            ObjectSupplier supplier
    ) {
        try {
            Class<?> targetClass = XposedHelpers.findClass(className, classLoader);
            XposedBridge.hookAllMethods(targetClass, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!isModuleActive(module)) {
                        return;
                    }
                    param.setResult(supplier.get());
                }
            });
            logEvent(packageName, module, "hook_installed", className + "." + methodName);
        } catch (Throwable throwable) {
            XposedBridge.log("SchoolRunDiag optional hook skipped: "
                    + className + "." + methodName + " " + throwable.getMessage());
        }
    }

    private boolean isModuleActive(RootDiagnosticModule module) {
        return active && activeModules.contains(module);
    }

    private static boolean shouldSkipPackage(String packageName) {
        return EXCLUDED_PACKAGES.contains(packageName)
                || packageName.startsWith("com.android.")
                || (packageName.startsWith("com.google.android.")
                && !"com.google.android.gms".equals(packageName));
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

    private interface ObjectSupplier {
        Object get();
    }

    private interface IntSupplier {
        int get();
    }

    private static final class LocationInjectionTarget {
        private final LocationListener listener;

        private LocationInjectionTarget(LocationListener listener) {
            this.listener = listener;
        }
    }

    private static final class SensorInjectionTarget {
        private final SensorEventListener listener;
        private final Sensor sensor;
        private final Handler handler;
        private long lastDetectorPulseMillis;

        private SensorInjectionTarget(SensorEventListener listener, Sensor sensor, Handler handler) {
            this.listener = listener;
            this.sensor = sensor;
            this.handler = handler;
        }
    }
}
