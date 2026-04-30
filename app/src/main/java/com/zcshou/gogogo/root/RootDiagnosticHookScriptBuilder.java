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
        return build(sessionId, targetPackageName, modules, RootDiagnosticSettings.defaults());
    }

    @NonNull
    public String build(
            @NonNull String sessionId,
            @NonNull String targetPackageName,
            @NonNull List<RootDiagnosticModule> modules,
            @NonNull RootDiagnosticSettings settings
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
        appendSettings(script, settings);
        appendCommonRuntime(script);
        if (contains(modules, RootDiagnosticModule.LOCATION_NMEA)) {
            appendLocationHooks(script, settings);
        }
        if (contains(modules, RootDiagnosticModule.RADIO_WIFI_SIGNAL)) {
            appendSignalHooks(script, settings);
        }
        if (contains(modules, RootDiagnosticModule.DETECTION_BYPASS)) {
            appendDetectionBypassHooks(script, settings);
        }
        if (contains(modules, RootDiagnosticModule.TARGET_APP_HOOK)) {
            appendTargetAppHooks(script, settings);
        }
        if (contains(modules, RootDiagnosticModule.SERVICE_STREAM)) {
            appendServiceStreamHooks(script, settings);
        }
        if (contains(modules, RootDiagnosticModule.SENSOR_INJECTION)) {
            appendSensorHooks(script, settings);
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

    private static void appendSettings(@NonNull StringBuilder script, @NonNull RootDiagnosticSettings settings) {
        script.append("  const DIAG_SETTINGS = {\n");
        script.append("    locationLatitude: ").append(jsNumber(settings.getLocationLatitude())).append(",\n");
        script.append("    locationLongitude: ").append(jsNumber(settings.getLocationLongitude())).append(",\n");
        script.append("    locationSpeed: ").append(jsNumber(settings.getLocationSpeedMetersPerSecond())).append(",\n");
        script.append("    locationSatellites: ").append(settings.getLocationSatellites()).append(",\n");
        script.append("    locationHdop: ").append(jsNumber(settings.getLocationHdop())).append(",\n");
        script.append("    wifiBssid: ").append(jsString(settings.getWifiBssid())).append(",\n");
        script.append("    wifiSsid: ").append(jsString(settings.getWifiSsid())).append(",\n");
        script.append("    wifiRssiDbm: ").append(settings.getWifiRssiDbm()).append(",\n");
        script.append("    wifiSignalLevel: ").append(settings.getSignalStrengthProfile().getWifiLevel()).append(",\n");
        script.append("    networkOperator: ").append(jsString(settings.getNetworkOperator())).append(",\n");
        script.append("    networkCountry: ").append(jsString(settings.getNetworkCountry())).append(",\n");
        script.append("    cellDbm: ").append(settings.getCellDbm()).append(",\n");
        script.append("    cellAsuLevel: ").append(settings.getSignalStrengthProfile().getCellAsuLevel()).append(",\n");
        script.append("    cellSignalLevel: ").append(settings.getSignalStrengthProfile().getCellLevel()).append(",\n");
        script.append("    bypassRootArtifacts: ").append(settings.isBypassRootArtifacts()).append(",\n");
        script.append("    bypassDebugger: ").append(settings.isBypassDebugger()).append(",\n");
        script.append("    bypassMockLocation: ").append(settings.isBypassMockLocation()).append(",\n");
        script.append("    targetHookMaxMethods: ").append(settings.getTargetHookMaxMethods()).append(",\n");
        script.append("    serviceClipboardNull: ").append(settings.isServiceClipboardNull()).append(",\n");
        script.append("    serviceBluetoothDisabled: ").append(settings.isServiceBluetoothDisabled()).append(",\n");
        script.append("    serviceNfcDisabled: ").append(settings.isServiceNfcDisabled()).append(",\n");
        script.append("    sensorMinCadence: ").append(jsNumber(settings.getSensorMinCadence())).append(",\n");
        script.append("    sensorMaxCadence: ").append(jsNumber(settings.getSensorMaxCadence())).append(",\n");
        script.append("    sensorWaveAmplitude: ").append(jsNumber(settings.getSensorWaveAmplitude())).append("\n");
        script.append("  };\n");
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

    private static void appendLocationHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installLocationNmeaHooks() {\n");
        script.append("    const Location = Java.use('android.location.Location');\n");
        script.append("    const Bundle = Java.use('android.os.Bundle');\n");
        script.append("    const SystemClock = Java.use('android.os.SystemClock');\n");
        script.append("    const mockLat = DIAG_SETTINGS.locationLatitude;\n");
        script.append("    const mockLon = DIAG_SETTINGS.locationLongitude;\n");
        script.append("    const mockSpeed = DIAG_SETTINGS.locationSpeed;\n");
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
        script.append("        extras.putInt('satellites', DIAG_SETTINGS.locationSatellites);\n");
        script.append("        extras.putFloat('hdop', DIAG_SETTINGS.locationHdop);\n");
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

    private static void appendSignalHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installRadioWifiSignalHooks() {\n");
        script.append("    const WifiInfo = tryUse('android.net.wifi.WifiInfo');\n");
        script.append("    if (WifiInfo) {\n");
        script.append("      WifiInfo.getBSSID.implementation = function () {\n");
        script.append("        emit('signal_simulation', 'value_override', 'WifiInfo.getBSSID -> ' + DIAG_SETTINGS.wifiBssid);\n");
        script.append("        return DIAG_SETTINGS.wifiBssid;\n");
        script.append("      };\n");
        script.append("      WifiInfo.getSSID.implementation = function () {\n");
        script.append("        emit('signal_simulation', 'value_override', 'WifiInfo.getSSID -> ' + DIAG_SETTINGS.wifiSsid);\n");
        script.append("        return '\"' + DIAG_SETTINGS.wifiSsid + '\"';\n");
        script.append("      };\n");
        script.append("      if (WifiInfo.getRssi) {\n");
        script.append("        WifiInfo.getRssi.implementation = function () {\n");
        script.append("          emit('signal_simulation', 'value_override', 'WifiInfo.getRssi -> ' + DIAG_SETTINGS.wifiRssiDbm);\n");
        script.append("          return DIAG_SETTINGS.wifiRssiDbm;\n");
        script.append("        };\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("    const SignalStrength = tryUse('android.telephony.SignalStrength');\n");
        script.append("    if (SignalStrength) {\n");
        script.append("      if (SignalStrength.getLevel) {\n");
        script.append("        SignalStrength.getLevel.implementation = function () { return DIAG_SETTINGS.cellSignalLevel; };\n");
        script.append("      }\n");
        script.append("      if (SignalStrength.getDbm) {\n");
        script.append("        SignalStrength.getDbm.implementation = function () { return DIAG_SETTINGS.cellDbm; };\n");
        script.append("      }\n");
        script.append("      if (SignalStrength.getAsuLevel) {\n");
        script.append("        SignalStrength.getAsuLevel.implementation = function () { return DIAG_SETTINGS.cellAsuLevel; };\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("    const TelephonyManager = tryUse('android.telephony.TelephonyManager');\n");
        script.append("    if (TelephonyManager) {\n");
        script.append("      ['getNetworkOperator', 'getSimOperator'].forEach(function (name) {\n");
        script.append("        if (TelephonyManager[name]) {\n");
        script.append("          TelephonyManager[name].implementation = function () {\n");
        script.append("            emit('signal_simulation', 'value_override', name + ' -> ' + DIAG_SETTINGS.networkOperator);\n");
        script.append("            return DIAG_SETTINGS.networkOperator;\n");
        script.append("          };\n");
        script.append("        }\n");
        script.append("      });\n");
        script.append("      if (TelephonyManager.getNetworkCountryIso) {\n");
        script.append("        TelephonyManager.getNetworkCountryIso.implementation = function () {\n");
        script.append("          emit('signal_simulation', 'value_override', 'getNetworkCountryIso -> ' + DIAG_SETTINGS.networkCountry);\n");
        script.append("          return DIAG_SETTINGS.networkCountry;\n");
        script.append("        };\n");
        script.append("      }\n");
        script.append("    }\n");
        script.append("  }\n\n");
    }

    private static void appendDetectionBypassHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installDetectionBypassHooks() {\n");
        script.append("    const File = Java.use('java.io.File');\n");
        script.append("    const exists = File.exists;\n");
        script.append("    exists.implementation = function () {\n");
        script.append("      const path = String(this.getAbsolutePath());\n");
        script.append("      if (DIAG_SETTINGS.bypassRootArtifacts && /\\/(su|magisk|busybox)$|xposed|frida|zygisk/i.test(path)) {\n");
        script.append("        emit('mock_location_bypass', 'return_override', 'File.exists false for ' + path);\n");
        script.append("        return false;\n");
        script.append("      }\n");
        script.append("      return exists.call(this);\n");
        script.append("    };\n");
        script.append("    const Debug = Java.use('android.os.Debug');\n");
        script.append("    const isDebuggerConnected = Debug.isDebuggerConnected;\n");
        script.append("    isDebuggerConnected.implementation = function () {\n");
        script.append("      if (!DIAG_SETTINGS.bypassDebugger) { return isDebuggerConnected.call(this); }\n");
        script.append("      emit('mock_location_bypass', 'return_override', 'Debug.isDebuggerConnected -> false');\n");
        script.append("      return false;\n");
        script.append("    };\n");
        script.append("    const SettingsSecure = Java.use('android.provider.Settings$Secure');\n");
        script.append("    const getString = SettingsSecure.getString.overload('android.content.ContentResolver', 'java.lang.String');\n");
        script.append("    getString.implementation = function (resolver, name) {\n");
        script.append("      if (DIAG_SETTINGS.bypassMockLocation && String(name) === 'mock_location') {\n");
        script.append("        emit('mock_location_bypass', 'return_override', 'Settings.Secure.mock_location -> 0');\n");
        script.append("        return '0';\n");
        script.append("      }\n");
        script.append("      return getString.call(this, resolver, name);\n");
        script.append("    };\n");
        script.append("    const Runtime = Java.use('java.lang.Runtime');\n");
        script.append("    Runtime.exec.overloads.forEach(function (overload) {\n");
        script.append("      overload.implementation = function () {\n");
        script.append("        const cmd = JSON.stringify(arguments[0]);\n");
        script.append("        if (DIAG_SETTINGS.bypassRootArtifacts && /su|magisk|getprop|mount|which/i.test(String(cmd))) {\n");
        script.append("          emit('mock_location_bypass', 'api_call', 'Runtime.exec detection command observed: ' + cmd);\n");
        script.append("        }\n");
        script.append("        return callOriginal(overload, this, arguments);\n");
        script.append("      };\n");
        script.append("    });\n");
        script.append("  }\n\n");
    }

    private static void appendTargetAppHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installTargetAppHookHooks() {\n");
        script.append("    const detectionClass = /(root|debug|emulator|mock|hook|frida|xposed|integrity|cheat)/i;\n");
        script.append("    const detectionMethod = /^(is|has|check|detect|verify|validate).*(root|debug|emulator|mock|hook|frida|xposed|integrity|cheat)?/i;\n");
        script.append("    let hooked = 0;\n");
        script.append("    Java.enumerateLoadedClasses({\n");
        script.append("      onMatch: function (name) {\n");
        script.append("        if (hooked >= DIAG_SETTINGS.targetHookMaxMethods || name.indexOf(TARGET_PACKAGE) !== 0 || !detectionClass.test(name)) {\n");
        script.append("          return;\n");
        script.append("        }\n");
        script.append("        try {\n");
        script.append("          const klass = Java.use(name);\n");
        script.append("          Object.keys(klass).forEach(function (methodName) {\n");
        script.append("            if (hooked >= DIAG_SETTINGS.targetHookMaxMethods || !detectionMethod.test(methodName) || !klass[methodName] || !klass[methodName].overloads) {\n");
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

    private static void appendServiceStreamHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installServiceStreamHooks() {\n");
        script.append("    const ClipboardManager = tryUse('android.content.ClipboardManager');\n");
        script.append("    if (ClipboardManager) {\n");
        script.append("      const getPrimaryClip = ClipboardManager.getPrimaryClip;\n");
        script.append("      ClipboardManager.getPrimaryClip.implementation = function () {\n");
        script.append("        if (!DIAG_SETTINGS.serviceClipboardNull) { return getPrimaryClip.call(this); }\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'ClipboardManager.getPrimaryClip -> null');\n");
        script.append("        return null;\n");
        script.append("      };\n");
        script.append("      const setPrimaryClip = ClipboardManager.setPrimaryClip;\n");
        script.append("      ClipboardManager.setPrimaryClip.implementation = function (clip) {\n");
        script.append("        if (!DIAG_SETTINGS.serviceClipboardNull) { return setPrimaryClip.call(this, clip); }\n");
        script.append("        emit('system_service_stream_log', 'data_blocked', 'ClipboardManager.setPrimaryClip suppressed');\n");
        script.append("        return;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("    const BluetoothAdapter = tryUse('android.bluetooth.BluetoothAdapter');\n");
        script.append("    if (BluetoothAdapter && BluetoothAdapter.isEnabled) {\n");
        script.append("      const bluetoothIsEnabled = BluetoothAdapter.isEnabled;\n");
        script.append("      BluetoothAdapter.isEnabled.implementation = function () {\n");
        script.append("        if (!DIAG_SETTINGS.serviceBluetoothDisabled) { return bluetoothIsEnabled.call(this); }\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'BluetoothAdapter.isEnabled -> false');\n");
        script.append("        return false;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("    const NfcAdapter = tryUse('android.nfc.NfcAdapter');\n");
        script.append("    if (NfcAdapter && NfcAdapter.isEnabled) {\n");
        script.append("      const nfcIsEnabled = NfcAdapter.isEnabled;\n");
        script.append("      NfcAdapter.isEnabled.implementation = function () {\n");
        script.append("        if (!DIAG_SETTINGS.serviceNfcDisabled) { return nfcIsEnabled.call(this); }\n");
        script.append("        emit('system_service_stream_log', 'return_override', 'NfcAdapter.isEnabled -> false');\n");
        script.append("        return false;\n");
        script.append("      };\n");
        script.append("    }\n");
        script.append("  }\n\n");
    }

    private static void appendSensorHooks(
            @NonNull StringBuilder script,
            @NonNull RootDiagnosticSettings settings
    ) {
        script.append("  function installSensorInjectionHooks() {\n");
        script.append("    const TWO_PI = Math.PI * 2;\n");
        script.append("    const sensorStartedAt = Date.now();\n");
        script.append("    const cadenceRange = Math.max(0, DIAG_SETTINGS.sensorMaxCadence - DIAG_SETTINGS.sensorMinCadence);\n");
        script.append("    const currentCadence = DIAG_SETTINGS.sensorMinCadence + (Math.random() * cadenceRange);\n");
        script.append("    let baseStepOffset = null;\n");
        script.append("    let lastLogSecond = -1;\n");
        script.append("    function sensorTypeOf(event) {\n");
        script.append("      try { return event.sensor.value.getType(); } catch (error) { return -1; }\n");
        script.append("    }\n");
        script.append("    function mutateSensorEvent(event, ownerName) {\n");
        script.append("      try {\n");
        script.append("        if (!event || !event.values || !event.values.value) { return; }\n");
        script.append("        const values = event.values.value;\n");
        script.append("        const type = sensorTypeOf(event);\n");
        script.append("        const elapsedSecs = Math.max(0, (Date.now() - sensorStartedAt) / 1000.0);\n");
        script.append("        const cadenceHz = Math.max(0.1, currentCadence / 60.0);\n");
        script.append("        const phase = elapsedSecs * TWO_PI * cadenceHz;\n");
        script.append("        if (type === 1 && values.length >= 3) {\n");
        script.append("          values[0] = Math.sin(phase) * 0.8;\n");
        script.append("          values[1] = Math.cos(phase * 0.5) * 0.6;\n");
        script.append("          values[2] = 9.81 + (Math.sin(phase) * DIAG_SETTINGS.sensorWaveAmplitude);\n");
        script.append("          const wholeSecond = Math.floor(elapsedSecs);\n");
        script.append("          if (wholeSecond !== lastLogSecond) {\n");
        script.append("            lastLogSecond = wholeSecond;\n");
        script.append("            emit('sensor_event_injection', 'data_injected', ownerName + ' accelerometer sine cadence=' + currentCadence.toFixed(0) + ' z=' + values[2].toFixed(2));\n");
        script.append("          }\n");
        script.append("        } else if ((type === 18 || type === 19) && values.length >= 1) {\n");
        script.append("          if (baseStepOffset === null) { baseStepOffset = values[0]; }\n");
        script.append("          const generatedSteps = Math.floor((elapsedSecs / 60.0) * currentCadence);\n");
        script.append("          values[0] = type === 18 ? 1.0 : baseStepOffset + generatedSteps;\n");
        script.append("          emit('sensor_event_injection', 'step_injected', ownerName + ' type=' + type + ' cadence=' + currentCadence.toFixed(0) + ' steps=' + values[0]);\n");
        script.append("        }\n");
        script.append("      } catch (error) { emit('sensor_event_injection', 'inject_error', error); }\n");
        script.append("    }\n");
        script.append("    let hooked = 0;\n");
        script.append("    const hookedListenerClasses = {};\n");
        script.append("    function hookSensorListenerClass(name) {\n");
        script.append("      if (!name || hookedListenerClasses[name] || hooked >= DIAG_SETTINGS.targetHookMaxMethods) { return; }\n");
        script.append("      try {\n");
        script.append("        const klass = Java.use(name);\n");
        script.append("        if (!klass.onSensorChanged || !klass.onSensorChanged.overloads) { return; }\n");
        script.append("        klass.onSensorChanged.overloads.forEach(function (overload) {\n");
        script.append("          if (overload.argumentTypes.length !== 1 || String(overload.argumentTypes[0].name) !== 'android.hardware.SensorEvent') { return; }\n");
        script.append("          overload.implementation = function (event) {\n");
        script.append("            mutateSensorEvent(event, name + '.onSensorChanged');\n");
        script.append("            return overload.call(this, event);\n");
        script.append("          };\n");
        script.append("          hooked += 1;\n");
        script.append("        });\n");
        script.append("        hookedListenerClasses[name] = true;\n");
        script.append("        emit('sensor_event_injection', 'listener_hooked', name);\n");
        script.append("      } catch (error) { emit('sensor_event_injection', 'hook_error', name + ': ' + error); }\n");
        script.append("    }\n");
        script.append("    const SensorManager = Java.use('android.hardware.SensorManager');\n");
        script.append("    SensorManager.registerListener.overloads.forEach(function (overload) {\n");
        script.append("      overload.implementation = function () {\n");
        script.append("        emit('sensor_event_injection', 'api_call', 'SensorManager.registerListener overload args=' + arguments.length);\n");
        script.append("        try {\n");
        script.append("          if (arguments.length > 0 && arguments[0]) {\n");
        script.append("            const listenerName = arguments[0].$className || arguments[0].getClass().getName();\n");
        script.append("            if (String(listenerName).indexOf(TARGET_PACKAGE) === 0) { hookSensorListenerClass(String(listenerName)); }\n");
        script.append("          }\n");
        script.append("        } catch (error) { emit('sensor_event_injection', 'listener_discovery_error', error); }\n");
        script.append("        return callOriginal(overload, this, arguments);\n");
        script.append("      };\n");
        script.append("    });\n");
        script.append("    Java.enumerateLoadedClasses({\n");
        script.append("      onMatch: function (name) {\n");
        script.append("        if (hooked >= DIAG_SETTINGS.targetHookMaxMethods || hookedListenerClasses[name] || name.indexOf(TARGET_PACKAGE) !== 0) {\n");
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
        script.append("                mutateSensorEvent(event, name + '.onSensorChanged');\n");
        script.append("              } catch (error) { emit('sensor_event_injection', 'inject_error', error); }\n");
        script.append("              return overload.call(this, event);\n");
        script.append("            };\n");
        script.append("            hooked += 1;\n");
        script.append("          });\n");
        script.append("          hookedListenerClasses[name] = true;\n");
        script.append("          emit('sensor_event_injection', 'listener_hooked', name);\n");
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
    private static String jsNumber(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return String.format(Locale.US, "%.6f", value);
    }

    @NonNull
    private static String shellQuote(@NonNull String value) {
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
