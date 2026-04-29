package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Locale;

public final class RootDiagnosticHookScriptBuilder {
    @NonNull
    public String build(
            @NonNull String sessionId,
            @NonNull String targetPackageName,
            @NonNull List<RootDiagnosticModule> modules
    ) {
        StringBuilder script = new StringBuilder(32 * 1024);
        script.append("'use strict';\n");
        script.append("(function () {\n");
        script.append("  const SESSION_ID = ").append(jsString(sessionId)).append(";\n");
        script.append("  const TARGET_PACKAGE = ").append(jsString(targetPackageName)).append(";\n");
        script.append("  const MODULES = {};\n");
        for (RootDiagnosticModule module : modules) {
            script.append("  MODULES[").append(jsString(module.getId())).append("] = true;\n");
        }
        appendCommonRuntime(script);
        if (contains(modules, RootDiagnosticModule.LOCATION_NMEA)) {
            appendLocationHooks(script);
        }
        if (contains(modules, RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
            appendSignalHooks(script);
        }
        if (contains(modules, RootDiagnosticModule.DETECTION_BYPASS)) {
            appendDetectionBypassHooks(script);
        }
        if (contains(modules, RootDiagnosticModule.TARGET_APP_HOOK)) {
            appendTargetAppHooks(script);
        }
        if (contains(modules, RootDiagnosticModule.SERVICE_STREAM)) {
            appendServiceStreamHooks(script);
        }
        if (contains(modules, RootDiagnosticModule.SENSOR_INJECTION)) {
            appendSensorHooks(script);
        }
        script.append("  if (!Java.available) {\n");
        script.append("    emit('framework', 'blocked', 'Java runtime unavailable; no hooks installed.');\n");
        script.append("    return;\n");
        script.append("  }\n");
        script.append("  Java.perform(function () {\n");
        script.append("    if (!guardTargetProcess()) {\n");
        script.append("      return;\n");
        script.append("    }\n");
        script.append("    emit('framework', 'target_guard_passed', 'Installing hooks for ' + TARGET_PACKAGE);\n");
        for (RootDiagnosticModule module : modules) {
            script.append("    safeInstall(")
                    .append(jsString(module.getId()))
                    .append(", install")
                    .append(functionSuffix(module))
                    .append("Hooks);\n");
        }
        script.append("    emit('framework', 'install_complete', 'Enabled modules: ' + Object.keys(MODULES).join(','));\n");
        script.append("  });\n");
        script.append("})();\n");
        return script.toString();
    }

    @NonNull
    public String buildManualAttachCommand(@NonNull String targetPackageName, @NonNull String scriptPath) {
        return "frida -U " + shellQuote(targetPackageName) + " -l " + shellQuote(scriptPath);
    }

    @NonNull
    public String buildManualSpawnCommand(@NonNull String targetPackageName, @NonNull String scriptPath) {
        return "frida -U -f " + shellQuote(targetPackageName) + " -l " + shellQuote(scriptPath);
    }

    private static void appendCommonRuntime(@NonNull StringBuilder script) {
        script.append("\n");
        script.append("  function emit(module, type, detail) {\n");
        script.append("    const payload = {\n");
        script.append("      sessionId: SESSION_ID,\n");
        script.append("      target: TARGET_PACKAGE,\n");
        script.append("      module: module,\n");
        script.append("      type: type,\n");
        script.append("      detail: String(detail),\n");
        script.append("      at: Date.now()\n");
        script.append("    };\n");
        script.append("    console.log('").append(RootDiagnosticEvent.FRIDA_PREFIX).append("' + JSON.stringify(payload));\n");
        script.append("  }\n\n");
        script.append("  function safeInstall(module, installer) {\n");
        script.append("    try {\n");
        script.append("      installer();\n");
        script.append("      emit(module, 'hook_installed', 'module hooks installed');\n");
        script.append("    } catch (error) {\n");
        script.append("      emit(module, 'hook_error', error && error.stack ? error.stack : error);\n");
        script.append("    }\n");
        script.append("  }\n\n");
        script.append("  function guardTargetProcess() {\n");
        script.append("    try {\n");
        script.append("      const ActivityThread = Java.use('android.app.ActivityThread');\n");
        script.append("      let currentPackage = ActivityThread.currentPackageName();\n");
        script.append("      if (!currentPackage) {\n");
        script.append("        const app = ActivityThread.currentApplication();\n");
        script.append("        currentPackage = app ? app.getPackageName() : '';\n");
        script.append("      }\n");
        script.append("      if (currentPackage !== TARGET_PACKAGE) {\n");
        script.append("        emit('framework', 'target_guard_blocked', 'current=' + currentPackage + ', expected=' + TARGET_PACKAGE);\n");
        script.append("        return false;\n");
        script.append("      }\n");
        script.append("      return true;\n");
        script.append("    } catch (error) {\n");
        script.append("      emit('framework', 'target_guard_error', error);\n");
        script.append("      return false;\n");
        script.append("    }\n");
        script.append("  }\n\n");
        script.append("  function tryUse(className) {\n");
        script.append("    try { return Java.use(className); } catch (error) { return null; }\n");
        script.append("  }\n\n");
        script.append("  function callOriginal(method, receiver, args) {\n");
        script.append("    return method.apply(receiver, args);\n");
        script.append("  }\n\n");
    }

    private static void appendLocationHooks(@NonNull StringBuilder script) {
        script.append("  function installLocationNmeaHooks() {\n");
        script.append("    const Location = Java.use('android.location.Location');\n");
        script.append("    const Bundle = Java.use('android.os.Bundle');\n");
        script.append("    const SystemClock = Java.use('android.os.SystemClock');\n");
        script.append("    const mockLat = 31.230416;\n");
        script.append("    const mockLon = 121.473701;\n");
        script.append("    const mockSpeed = 3.8;\n");
        script.append("    function stamp(location) {\n");
        script.append("      try {\n");
        script.append("        location.setLatitude(mockLat);\n");
        script.append("        location.setLongitude(mockLon);\n");
        script.append("        location.setAccuracy(4.2);\n");
        script.append("        location.setSpeed(mockSpeed);\n");
        script.append("        location.setBearing(86.0);\n");
        script.append("        location.setTime(Date.now());\n");
        script.append("        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());\n");
        script.append("        const extras = Bundle.$new();\n");
        script.append("        extras.putInt('satellites', 9);\n");
        script.append("        extras.putFloat('hdop', 0.8);\n");
        script.append("        extras.putString('nmea', '$GPRMC,000000,A,3113.8250,N,12128.4221,E,007.4,086.0,290426,,,A*00');\n");
        script.append("        location.setExtras(extras);\n");
        script.append("      } catch (error) { emit('root_nmea_injection', 'location_stamp_error', error); }\n");
        script.append("      return location;\n");
        script.append("    }\n");
        script.append("    const ctor = Location.$init.overload('java.lang.String');\n");
        script.append("    ctor.implementation = function (provider) {\n");
        script.append("      const result = ctor.call(this, provider);\n");
        script.append("      emit('root_nmea_injection', 'location_created', 'provider=' + provider);\n");
        script.append("      return result;\n");
        script.append("    };\n");
        script.append("    const LocationManager = Java.use('android.location.LocationManager');\n");
        script.append("    const getLastKnownLocation = LocationManager.getLastKnownLocation.overload('java.lang.String');\n");
        script.append("    getLastKnownLocation.implementation = function (provider) {\n");
        script.append("      const original = getLastKnownLocation.call(this, provider);\n");
        script.append("      const location = original ? original : Location.$new(provider ? provider : 'gps');\n");
        script.append("      emit('root_nmea_injection', 'return_override', 'getLastKnownLocation(' + provider + ') -> mock GPS/NMEA sample');\n");
        script.append("      return stamp(location);\n");
        script.append("    };\n");
        script.append("    LocationManager.requestLocationUpdates.overloads.forEach(function (overload) {\n");
        script.append("      overload.implementation = function () {\n");
        script.append("        emit('root_nmea_injection', 'api_call', 'requestLocationUpdates overload args=' + arguments.length);\n");
        script.append("        return callOriginal(overload, this, arguments);\n");
        script.append("      };\n");
        script.append("    });\n");
        script.append("    const getLatitude = Location.getLatitude;\n");
        script.append("    getLatitude.implementation = function () {\n");
        script.append("      emit('root_nmea_injection', 'value_override', 'Location.getLatitude -> ' + mockLat);\n");
        script.append("      return mockLat;\n");
        script.append("    };\n");
        script.append("    const getLongitude = Location.getLongitude;\n");
        script.append("    getLongitude.implementation = function () {\n");
        script.append("      emit('root_nmea_injection', 'value_override', 'Location.getLongitude -> ' + mockLon);\n");
        script.append("      return mockLon;\n");
        script.append("    };\n");
        script.append("  }\n\n");
    }

    private static void appendSignalHooks(@NonNull StringBuilder script) {
        script.append("  function installRadioWifiSignalHooks() {\n");
        script.append("    const WifiInfo = tryUse('android.net.wifi.WifiInfo');\n");
        script.append("    if (WifiInfo) {\n");
        script.append("      WifiInfo.getBSSID.implementation = function () {\n");
        script.append("        emit('signal_simulation', 'value_override', 'WifiInfo.getBSSID -> 02:00:00:7a:11:29');\n");
        script.append("        return '02:00:00:7a:11:29';\n");
        script.append("      };\n");
        script.append("      WifiInfo.getSSID.implementation = function () {\n");
        script.append("        emit('signal_simulation', 'value_override', 'WifiInfo.getSSID -> Internal-Test-WiFi');\n");
        script.append("        return '\"Internal-Test-WiFi\"';\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("    const TelephonyManager = tryUse('android.telephony.TelephonyManager');\n");
        script.append("    if (TelephonyManager) {\n");
        script.append("      ['getNetworkOperator', 'getSimOperator'].forEach(function (name) {\n");
        script.append("        if (TelephonyManager[name]) {\n");
        script.append("          TelephonyManager[name].implementation = function () {\n");
        script.append("            emit('signal_simulation', 'value_override', name + ' -> 46000');\n");
        script.append("            return '46000';\n");
        script.append("          };\n");
        script.append("        }\n");
        script.append("      });\n");
        script.append("      if (TelephonyManager.getNetworkCountryIso) {\n");
        script.append("        TelephonyManager.getNetworkCountryIso.implementation = function () {\n");
        script.append("          emit('signal_simulation', 'value_override', 'getNetworkCountryIso -> cn');\n");
        script.append("          return 'cn';\n");
        script.append("        };\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("  }\n\n");
    }

    private static void appendDetectionBypassHooks(@NonNull StringBuilder script) {
        script.append("  function installDetectionBypassHooks() {\n");
        script.append("    const File = Java.use('java.io.File');\n");
        script.append("    const exists = File.exists;\n");
        script.append("    exists.implementation = function () {\n");
        script.append("      const path = String(this.getAbsolutePath());\n");
        script.append("      if (/\\/(su|magisk|busybox)$|xposed|frida|zygisk/i.test(path)) {\n");
        script.append("        emit('mock_location_bypass', 'return_override', 'File.exists false for ' + path);\n");
        script.append("        return false;\n");
        script.append("      }\n");
        script.append("      return exists.call(this);\n");
        script.append("    };\n");
        script.append("    const Debug = Java.use('android.os.Debug');\n");
        script.append("    Debug.isDebuggerConnected.implementation = function () {\n");
        script.append("      emit('mock_location_bypass', 'return_override', 'Debug.isDebuggerConnected -> false');\n");
        script.append("      return false;\n");
        script.append("    };\n");
        script.append("    const SettingsSecure = Java.use('android.provider.Settings$Secure');\n");
        script.append("    const getString = SettingsSecure.getString.overload('android.content.ContentResolver', 'java.lang.String');\n");
        script.append("    getString.implementation = function (resolver, name) {\n");
        script.append("      if (String(name) === 'mock_location') {\n");
        script.append("        emit('mock_location_bypass', 'return_override', 'Settings.Secure.mock_location -> 0');\n");
        script.append("        return '0';\n");
        script.append("      }\n");
        script.append("      return getString.call(this, resolver, name);\n");
        script.append("    };\n");
        script.append("    const Runtime = Java.use('java.lang.Runtime');\n");
        script.append("    Runtime.exec.overloads.forEach(function (overload) {\n");
        script.append("      overload.implementation = function () {\n");
        script.append("        const cmd = JSON.stringify(arguments[0]);\n");
        script.append("        if (/su|magisk|getprop|mount|which/i.test(String(cmd))) {\n");
        script.append("          emit('mock_location_bypass', 'api_call', 'Runtime.exec detection command observed: ' + cmd);\n");
        script.append("        }\n");
        script.append("        return callOriginal(overload, this, arguments);\n");
        script.append("      };\n");
        script.append("    });\n");
        script.append("  }\n\n");
    }

    private static void appendTargetAppHooks(@NonNull StringBuilder script) {
        script.append("  function installTargetAppHookHooks() {\n");
        script.append("    const detectionClass = /(root|debug|emulator|mock|hook|frida|xposed|integrity|cheat)/i;\n");
        script.append("    const detectionMethod = /^(is|has|check|detect|verify|validate).*(root|debug|emulator|mock|hook|frida|xposed|integrity|cheat)?/i;\n");
        script.append("    let hooked = 0;\n");
        script.append("    Java.enumerateLoadedClasses({\n");
        script.append("      onMatch: function (name) {\n");
        script.append("        if (hooked >= 80 || name.indexOf(TARGET_PACKAGE) !== 0 || !detectionClass.test(name)) {\n");
        script.append("          return;\n");
        script.append("        }\n");
        script.append("        try {\n");
        script.append("          const klass = Java.use(name);\n");
        script.append("          Object.keys(klass).forEach(function (methodName) {\n");
        script.append("            if (hooked >= 80 || !detectionMethod.test(methodName) || !klass[methodName] || !klass[methodName].overloads) {\n");
        script.append("              return;\n");
        script.append("            }\n");
        script.append("            klass[methodName].overloads.forEach(function (overload) {\n");
        script.append("              if (String(overload.returnType.name) !== 'boolean') {\n");
        script.append("                return;\n");
        script.append("              }\n");
        script.append("              overload.implementation = function () {\n");
        script.append("                emit('target_app_hook', 'return_override', name + '.' + methodName + ' -> false');\n");
        script.append("                return false;\n");
        script.append("              };\n");
        script.append("              hooked += 1;\n");
        script.append("            });\n");
        script.append("          });\n");
        script.append("        } catch (error) {\n");
        script.append("          emit('target_app_hook', 'hook_error', name + ': ' + error);\n");
        script.append("        }\n");
        script.append("      },\n");
        script.append("      onComplete: function () {\n");
        script.append("        emit('target_app_hook', 'scan_complete', 'boolean detection methods hooked=' + hooked);\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  }\n\n");
    }

    private static void appendServiceStreamHooks(@NonNull StringBuilder script) {
        script.append("  function installServiceStreamHooks() {\n");
        script.append("    const ClipboardManager = tryUse('android.content.ClipboardManager');\n");
        script.append("    if (ClipboardManager) {\n");
        script.append("      ClipboardManager.getPrimaryClip.implementation = function () {\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'ClipboardManager.getPrimaryClip -> null');\n");
        script.append("        return null;\n");
        script.append("      };\n");
        script.append("      ClipboardManager.setPrimaryClip.implementation = function (clip) {\n");
        script.append("        emit('system_service_stream_log', 'data_blocked', 'ClipboardManager.setPrimaryClip suppressed');\n");
        script.append("        return;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("    const BluetoothAdapter = tryUse('android.bluetooth.BluetoothAdapter');\n");
        script.append("    if (BluetoothAdapter && BluetoothAdapter.isEnabled) {\n");
        script.append("      BluetoothAdapter.isEnabled.implementation = function () {\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'BluetoothAdapter.isEnabled -> false');\n");
        script.append("        return false;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("    const NfcAdapter = tryUse('android.nfc.NfcAdapter');\n");
        script.append("    if (NfcAdapter && NfcAdapter.isEnabled) {\n");
        script.append("      NfcAdapter.isEnabled.implementation = function () {\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'NfcAdapter.isEnabled -> false');\n");
        script.append("        return false;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("  }\n\n");
    }

    private static void appendSensorHooks(@NonNull StringBuilder script) {
        script.append("  function installSensorInjectionHooks() {\n");
        script.append("    const SensorManager = Java.use('android.hardware.SensorManager');\n");
        script.append("    SensorManager.registerListener.overloads.forEach(function (overload) {\n");
        script.append("      overload.implementation = function () {\n");
        script.append("        emit('sensor_event_injection', 'api_call', 'SensorManager.registerListener overload args=' + arguments.length);\n");
        script.append("        return callOriginal(overload, this, arguments);\n");
        script.append("      };\n");
        script.append("    });\n");
        script.append("    let hooked = 0;\n");
        script.append("    Java.enumerateLoadedClasses({\n");
        script.append("      onMatch: function (name) {\n");
        script.append("        if (hooked >= 80 || name.indexOf(TARGET_PACKAGE) !== 0) {\n");
        script.append("          return;\n");
        script.append("        }\n");
        script.append("        try {\n");
        script.append("          const klass = Java.use(name);\n");
        script.append("          if (!klass.onSensorChanged || !klass.onSensorChanged.overloads) {\n");
        script.append("            return;\n");
        script.append("          }\n");
        script.append("          klass.onSensorChanged.overloads.forEach(function (overload) {\n");
        script.append("            if (overload.argumentTypes.length !== 1 || String(overload.argumentTypes[0].name) !== 'android.hardware.SensorEvent') {\n");
        script.append("              return;\n");
        script.append("            }\n");
        script.append("            overload.implementation = function (event) {\n");
        script.append("              try {\n");
        script.append("                if (event && event.values && event.values.value && event.values.value.length >= 3) {\n");
        script.append("                  event.values.value[0] = 24.8;\n");
        script.append("                  event.values.value[1] = -18.6;\n");
        script.append("                  event.values.value[2] = 0.4;\n");
        script.append("                  emit('sensor_event_injection', 'data_injected', name + '.onSensorChanged acceleration spike');\n");
        script.append("                }\n");
        script.append("              } catch (error) { emit('sensor_event_injection', 'inject_error', error); }\n");
        script.append("              return overload.call(this, event);\n");
        script.append("            };\n");
        script.append("            hooked += 1;\n");
        script.append("          });\n");
        script.append("        } catch (error) {\n");
        script.append("          emit('sensor_event_injection', 'hook_error', name + ': ' + error);\n");
        script.append("        }\n");
        script.append("      },\n");
        script.append("      onComplete: function () {\n");
        script.append("        emit('sensor_event_injection', 'scan_complete', 'sensor listeners hooked=' + hooked);\n");
        script.append("      }\n");
        script.append("    });\n");
        script.append("  }\n\n");
    }

    private static boolean contains(@NonNull List<RootDiagnosticModule> modules, @NonNull RootDiagnosticModule expected) {
        for (RootDiagnosticModule module : modules) {
            if (module == expected) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String functionSuffix(@NonNull RootDiagnosticModule module) {
        switch (module) {
            case LOCATION_NMEA:
                return "LocationNmea";
            case RADIO_WIFI_SIGNAL:
                return "RadioWifiSignal";
            case DETECTION_BYPASS:
                return "DetectionBypass";
            case TARGET_APP_HOOK:
                return "TargetAppHook";
            case SERVICE_STREAM:
                return "ServiceStream";
            case SENSOR_INJECTION:
                return "SensorInjection";
            default:
                throw new IllegalArgumentException(String.format(Locale.US, "Unknown module %s", module.name()));
        }
    }

    @NonNull
    private static String jsString(@NonNull String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        return "\"" + escaped + "\"";
    }

    @NonNull
    private static String shellQuote(@NonNull String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
