package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Locale;

public final class RootDiagnosticSettings {
    public static final double MIN_SENSOR_CADENCE = 140d;
    public static final double MAX_SENSOR_CADENCE = 220d;

    private static final String KEY_LOCATION_LATITUDE = "locationLatitude";
    private static final String KEY_LOCATION_LONGITUDE = "locationLongitude";
    private static final String KEY_LOCATION_SPEED = "locationSpeed";
    private static final String KEY_LOCATION_ALTITUDE = "locationAltitude";
    private static final String KEY_LOCATION_BEARING = "locationBearing";
    private static final String KEY_LOCATION_SATELLITES = "locationSatellites";
    private static final String KEY_LOCATION_HDOP = "locationHdop";
    private static final String KEY_WIFI_BSSID = "wifiBssid";
    private static final String KEY_WIFI_SSID = "wifiSsid";
    private static final String KEY_NETWORK_OPERATOR = "networkOperator";
    private static final String KEY_NETWORK_COUNTRY = "networkCountry";
    private static final String KEY_BYPASS_ROOT_ARTIFACTS = "bypassRootArtifacts";
    private static final String KEY_BYPASS_DEBUGGER = "bypassDebugger";
    private static final String KEY_BYPASS_MOCK_LOCATION = "bypassMockLocation";
    private static final String KEY_TARGET_HOOK_MAX_METHODS = "targetHookMaxMethods";
    private static final String KEY_SERVICE_CLIPBOARD_NULL = "serviceClipboardNull";
    private static final String KEY_SERVICE_BLUETOOTH_DISABLED = "serviceBluetoothDisabled";
    private static final String KEY_SERVICE_NFC_DISABLED = "serviceNfcDisabled";
    private static final String KEY_SENSOR_MIN_CADENCE = "sensorMinCadence";
    private static final String KEY_SENSOR_MAX_CADENCE = "sensorMaxCadence";
    private static final String KEY_SENSOR_WAVE_AMPLITUDE = "sensorWaveAmplitude";

    private final double locationLatitude;
    private final double locationLongitude;
    private final double locationSpeedMetersPerSecond;
    private final double locationAltitudeMeters;
    private final float locationBearingDegrees;
    private final int locationSatellites;
    private final double locationHdop;
    private final String wifiBssid;
    private final String wifiSsid;
    private final String networkOperator;
    private final String networkCountry;
    private final RootSignalStrengthProfile signalStrengthProfile;
    private final boolean bypassRootArtifacts;
    private final boolean bypassDebugger;
    private final boolean bypassMockLocation;
    private final int targetHookMaxMethods;
    private final boolean serviceClipboardNull;
    private final boolean serviceBluetoothDisabled;
    private final boolean serviceNfcDisabled;
    private final double sensorMinCadence;
    private final double sensorMaxCadence;
    private final double sensorWaveAmplitude;

    public RootDiagnosticSettings(
            double locationLatitude,
            double locationLongitude,
            double locationSpeedMetersPerSecond,
            double locationAltitudeMeters,
            float locationBearingDegrees,
            int locationSatellites,
            double locationHdop,
            @NonNull String wifiBssid,
            @NonNull String wifiSsid,
            @NonNull String networkOperator,
            @NonNull String networkCountry,
            @NonNull RootSignalStrengthProfile signalStrengthProfile,
            boolean bypassRootArtifacts,
            boolean bypassDebugger,
            boolean bypassMockLocation,
            int targetHookMaxMethods,
            boolean serviceClipboardNull,
            boolean serviceBluetoothDisabled,
            boolean serviceNfcDisabled,
            double sensorMinCadence,
            double sensorMaxCadence,
            double sensorWaveAmplitude
    ) {
        this.locationLatitude = clamp(locationLatitude, -90d, 90d);
        this.locationLongitude = clamp(locationLongitude, -180d, 180d);
        this.locationSpeedMetersPerSecond = clamp(locationSpeedMetersPerSecond, 0d, 25d);
        this.locationAltitudeMeters = clamp(locationAltitudeMeters, -500d, 9000d);
        this.locationBearingDegrees = normalizeBearing(locationBearingDegrees);
        this.locationSatellites = clamp(locationSatellites, 1, 32);
        this.locationHdop = clamp(locationHdop, 0.3d, 9.9d);
        this.wifiBssid = sanitize(wifiBssid, "02:00:00:7a:11:29");
        this.wifiSsid = sanitize(wifiSsid, "Internal-Test-WiFi");
        this.networkOperator = sanitize(networkOperator, "46000");
        this.networkCountry = sanitize(networkCountry, "cn");
        this.signalStrengthProfile = signalStrengthProfile;
        this.bypassRootArtifacts = bypassRootArtifacts;
        this.bypassDebugger = bypassDebugger;
        this.bypassMockLocation = bypassMockLocation;
        this.targetHookMaxMethods = clamp(targetHookMaxMethods, 1, 200);
        this.serviceClipboardNull = serviceClipboardNull;
        this.serviceBluetoothDisabled = serviceBluetoothDisabled;
        this.serviceNfcDisabled = serviceNfcDisabled;
        double minCadence = clamp(sensorMinCadence, MIN_SENSOR_CADENCE, MAX_SENSOR_CADENCE);
        double maxCadence = clamp(sensorMaxCadence, MIN_SENSOR_CADENCE, MAX_SENSOR_CADENCE);
        this.sensorMinCadence = Math.min(minCadence, maxCadence);
        this.sensorMaxCadence = Math.max(minCadence, maxCadence);
        this.sensorWaveAmplitude = clamp(sensorWaveAmplitude, 0.5d, 8.0d);
    }

    @NonNull
    public static RootDiagnosticSettings defaults() {
        return new RootDiagnosticSettings(
                31.230416d,
                121.473701d,
                3.8d,
                55d,
                0f,
                9,
                0.8d,
                "02:00:00:7a:11:29",
                "Internal-Test-WiFi",
                "46000",
                "cn",
                RootSignalStrengthProfile.defaults(),
                true,
                true,
                true,
                80,
                true,
                true,
                true,
                172d,
                182d,
                3.5d
        );
    }

    @NonNull
    public static RootDiagnosticSettings fromJson(@Nullable String rawJson) {
        RootDiagnosticSettings defaults = defaults();
        if (rawJson == null || rawJson.trim().isEmpty()) {
            return defaults;
        }
        try {
            JSONObject object = new JSONObject(rawJson);
            return new RootDiagnosticSettings(
                    object.optDouble(KEY_LOCATION_LATITUDE, defaults.getLocationLatitude()),
                    object.optDouble(KEY_LOCATION_LONGITUDE, defaults.getLocationLongitude()),
                    object.optDouble(KEY_LOCATION_SPEED, defaults.getLocationSpeedMetersPerSecond()),
                    object.optDouble(KEY_LOCATION_ALTITUDE, defaults.getLocationAltitudeMeters()),
                    (float) object.optDouble(KEY_LOCATION_BEARING, defaults.getLocationBearingDegrees()),
                    object.optInt(KEY_LOCATION_SATELLITES, defaults.getLocationSatellites()),
                    object.optDouble(KEY_LOCATION_HDOP, defaults.getLocationHdop()),
                    object.optString(KEY_WIFI_BSSID, defaults.getWifiBssid()),
                    object.optString(KEY_WIFI_SSID, defaults.getWifiSsid()),
                    object.optString(KEY_NETWORK_OPERATOR, defaults.getNetworkOperator()),
                    object.optString(KEY_NETWORK_COUNTRY, defaults.getNetworkCountry()),
                    RootSignalStrengthProfile.fromJson(object, defaults.getSignalStrengthProfile()),
                    object.optBoolean(KEY_BYPASS_ROOT_ARTIFACTS, defaults.isBypassRootArtifacts()),
                    object.optBoolean(KEY_BYPASS_DEBUGGER, defaults.isBypassDebugger()),
                    object.optBoolean(KEY_BYPASS_MOCK_LOCATION, defaults.isBypassMockLocation()),
                    object.optInt(KEY_TARGET_HOOK_MAX_METHODS, defaults.getTargetHookMaxMethods()),
                    object.optBoolean(KEY_SERVICE_CLIPBOARD_NULL, defaults.isServiceClipboardNull()),
                    object.optBoolean(KEY_SERVICE_BLUETOOTH_DISABLED, defaults.isServiceBluetoothDisabled()),
                    object.optBoolean(KEY_SERVICE_NFC_DISABLED, defaults.isServiceNfcDisabled()),
                    object.optDouble(KEY_SENSOR_MIN_CADENCE, defaults.getSensorMinCadence()),
                    object.optDouble(KEY_SENSOR_MAX_CADENCE, defaults.getSensorMaxCadence()),
                    object.optDouble(KEY_SENSOR_WAVE_AMPLITUDE, defaults.getSensorWaveAmplitude())
            );
        } catch (Exception ignored) {
            return defaults;
        }
    }

    @NonNull
    public String toJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_LOCATION_LATITUDE, locationLatitude);
            object.put(KEY_LOCATION_LONGITUDE, locationLongitude);
            object.put(KEY_LOCATION_SPEED, locationSpeedMetersPerSecond);
            object.put(KEY_LOCATION_ALTITUDE, locationAltitudeMeters);
            object.put(KEY_LOCATION_BEARING, locationBearingDegrees);
            object.put(KEY_LOCATION_SATELLITES, locationSatellites);
            object.put(KEY_LOCATION_HDOP, locationHdop);
            object.put(KEY_WIFI_BSSID, wifiBssid);
            object.put(KEY_WIFI_SSID, wifiSsid);
            object.put(KEY_NETWORK_OPERATOR, networkOperator);
            object.put(KEY_NETWORK_COUNTRY, networkCountry);
            signalStrengthProfile.writeToJson(object);
            object.put(KEY_BYPASS_ROOT_ARTIFACTS, bypassRootArtifacts);
            object.put(KEY_BYPASS_DEBUGGER, bypassDebugger);
            object.put(KEY_BYPASS_MOCK_LOCATION, bypassMockLocation);
            object.put(KEY_TARGET_HOOK_MAX_METHODS, targetHookMaxMethods);
            object.put(KEY_SERVICE_CLIPBOARD_NULL, serviceClipboardNull);
            object.put(KEY_SERVICE_BLUETOOTH_DISABLED, serviceBluetoothDisabled);
            object.put(KEY_SERVICE_NFC_DISABLED, serviceNfcDisabled);
            object.put(KEY_SENSOR_MIN_CADENCE, sensorMinCadence);
            object.put(KEY_SENSOR_MAX_CADENCE, sensorMaxCadence);
            object.put(KEY_SENSOR_WAVE_AMPLITUDE, sensorWaveAmplitude);
        } catch (Exception ignored) {
            // Keep best-effort local persistence.
        }
        return object.toString();
    }

    @NonNull
    public String summarize(@NonNull RootDiagnosticModule module) {
        switch (module) {
            case LOCATION_NMEA:
                return String.format(Locale.getDefault(), "lat=%.6f, lon=%.6f, speed=%.1fm/s, alt=%.1fm, bearing=%.1f, satellites=%d, hdop=%.1f",
                        locationLatitude, locationLongitude, locationSpeedMetersPerSecond,
                        locationAltitudeMeters, locationBearingDegrees, locationSatellites, locationHdop);
            case RADIO_WIFI_SIGNAL:
                return "ssid=" + wifiSsid
                        + ", bssid=" + wifiBssid
                        + ", operator=" + networkOperator
                        + ", country=" + networkCountry
                        + ", " + signalStrengthProfile.summarize();
            case DETECTION_BYPASS:
                return "root=" + bypassRootArtifacts + ", debugger=" + bypassDebugger + ", mockLocation=" + bypassMockLocation;
            case TARGET_APP_HOOK:
                return "maxBooleanHooks=" + targetHookMaxMethods;
            case SERVICE_STREAM:
                return "clipboardNull=" + serviceClipboardNull + ", bluetoothDisabled=" + serviceBluetoothDisabled + ", nfcDisabled=" + serviceNfcDisabled;
            case SENSOR_INJECTION:
                return String.format(Locale.getDefault(), "cadence=%.0f-%.0fSPM, wave=%.1f", sensorMinCadence, sensorMaxCadence, sensorWaveAmplitude);
            default:
                return "";
        }
    }

    public double getLocationLatitude() {
        return locationLatitude;
    }

    public double getLocationLongitude() {
        return locationLongitude;
    }

    public double getLocationSpeedMetersPerSecond() {
        return locationSpeedMetersPerSecond;
    }

    public double getLocationAltitudeMeters() {
        return locationAltitudeMeters;
    }

    public float getLocationBearingDegrees() {
        return locationBearingDegrees;
    }

    public int getLocationSatellites() {
        return locationSatellites;
    }

    public double getLocationHdop() {
        return locationHdop;
    }

    @NonNull
    public String getWifiBssid() {
        return wifiBssid;
    }

    @NonNull
    public String getWifiSsid() {
        return wifiSsid;
    }

    @NonNull
    public String getNetworkOperator() {
        return networkOperator;
    }

    @NonNull
    public String getNetworkCountry() {
        return networkCountry;
    }

    @NonNull
    public RootSignalStrengthProfile getSignalStrengthProfile() {
        return signalStrengthProfile;
    }

    public int getWifiRssiDbm() {
        return signalStrengthProfile.getWifiRssiDbm();
    }

    public int getWifiJitterDbm() {
        return signalStrengthProfile.getWifiJitterDbm();
    }

    public int getCellDbm() {
        return signalStrengthProfile.getCellDbm();
    }

    public int getCellJitterDbm() {
        return signalStrengthProfile.getCellJitterDbm();
    }

    public boolean isBypassRootArtifacts() {
        return bypassRootArtifacts;
    }

    public boolean isBypassDebugger() {
        return bypassDebugger;
    }

    public boolean isBypassMockLocation() {
        return bypassMockLocation;
    }

    public int getTargetHookMaxMethods() {
        return targetHookMaxMethods;
    }

    public boolean isServiceClipboardNull() {
        return serviceClipboardNull;
    }

    public boolean isServiceBluetoothDisabled() {
        return serviceBluetoothDisabled;
    }

    public boolean isServiceNfcDisabled() {
        return serviceNfcDisabled;
    }

    public double getSensorMinCadence() {
        return sensorMinCadence;
    }

    public double getSensorMaxCadence() {
        return sensorMaxCadence;
    }

    public double getSensorWaveAmplitude() {
        return sensorWaveAmplitude;
    }

    @NonNull
    public RootDiagnosticSettings withLocation(
            double latitude,
            double longitude,
            double speedMetersPerSecond,
            int satellites,
            double hdop
    ) {
        return withLocation(
                latitude,
                longitude,
                speedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                satellites,
                hdop
        );
    }

    @NonNull
    public RootDiagnosticSettings withLocation(
            double latitude,
            double longitude,
            double speedMetersPerSecond,
            double altitudeMeters,
            float bearingDegrees,
            int satellites,
            double hdop
    ) {
        return new RootDiagnosticSettings(
                latitude,
                longitude,
                speedMetersPerSecond,
                altitudeMeters,
                bearingDegrees,
                satellites,
                hdop,
                wifiBssid,
                wifiSsid,
                networkOperator,
                networkCountry,
                signalStrengthProfile,
                bypassRootArtifacts,
                bypassDebugger,
                bypassMockLocation,
                targetHookMaxMethods,
                serviceClipboardNull,
                serviceBluetoothDisabled,
                serviceNfcDisabled,
                sensorMinCadence,
                sensorMaxCadence,
                sensorWaveAmplitude
        );
    }

    @NonNull
    public RootDiagnosticSettings withSignal(
            @NonNull String bssid,
            @NonNull String ssid,
            @NonNull String operator,
            @NonNull String country
    ) {
        return withSignal(bssid, ssid, operator, country, signalStrengthProfile);
    }

    @NonNull
    public RootDiagnosticSettings withSignal(
            @NonNull String bssid,
            @NonNull String ssid,
            @NonNull String operator,
            @NonNull String country,
            @NonNull RootSignalStrengthProfile signalStrengthProfile
    ) {
        return new RootDiagnosticSettings(
                locationLatitude,
                locationLongitude,
                locationSpeedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                locationSatellites,
                locationHdop,
                bssid,
                ssid,
                operator,
                country,
                signalStrengthProfile,
                bypassRootArtifacts,
                bypassDebugger,
                bypassMockLocation,
                targetHookMaxMethods,
                serviceClipboardNull,
                serviceBluetoothDisabled,
                serviceNfcDisabled,
                sensorMinCadence,
                sensorMaxCadence,
                sensorWaveAmplitude
        );
    }

    @NonNull
    public RootDiagnosticSettings withBypass(
            boolean rootArtifacts,
            boolean debugger,
            boolean mockLocation
    ) {
        return new RootDiagnosticSettings(
                locationLatitude,
                locationLongitude,
                locationSpeedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                locationSatellites,
                locationHdop,
                wifiBssid,
                wifiSsid,
                networkOperator,
                networkCountry,
                signalStrengthProfile,
                rootArtifacts,
                debugger,
                mockLocation,
                targetHookMaxMethods,
                serviceClipboardNull,
                serviceBluetoothDisabled,
                serviceNfcDisabled,
                sensorMinCadence,
                sensorMaxCadence,
                sensorWaveAmplitude
        );
    }

    @NonNull
    public RootDiagnosticSettings withTargetHookMaxMethods(int maxMethods) {
        return new RootDiagnosticSettings(
                locationLatitude,
                locationLongitude,
                locationSpeedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                locationSatellites,
                locationHdop,
                wifiBssid,
                wifiSsid,
                networkOperator,
                networkCountry,
                signalStrengthProfile,
                bypassRootArtifacts,
                bypassDebugger,
                bypassMockLocation,
                maxMethods,
                serviceClipboardNull,
                serviceBluetoothDisabled,
                serviceNfcDisabled,
                sensorMinCadence,
                sensorMaxCadence,
                sensorWaveAmplitude
        );
    }

    @NonNull
    public RootDiagnosticSettings withServiceStream(
            boolean clipboardNull,
            boolean bluetoothDisabled,
            boolean nfcDisabled
    ) {
        return new RootDiagnosticSettings(
                locationLatitude,
                locationLongitude,
                locationSpeedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                locationSatellites,
                locationHdop,
                wifiBssid,
                wifiSsid,
                networkOperator,
                networkCountry,
                signalStrengthProfile,
                bypassRootArtifacts,
                bypassDebugger,
                bypassMockLocation,
                targetHookMaxMethods,
                clipboardNull,
                bluetoothDisabled,
                nfcDisabled,
                sensorMinCadence,
                sensorMaxCadence,
                sensorWaveAmplitude
        );
    }

    @NonNull
    public RootDiagnosticSettings withSensor(
            double minCadence,
            double maxCadence,
            double waveAmplitude
    ) {
        return new RootDiagnosticSettings(
                locationLatitude,
                locationLongitude,
                locationSpeedMetersPerSecond,
                locationAltitudeMeters,
                locationBearingDegrees,
                locationSatellites,
                locationHdop,
                wifiBssid,
                wifiSsid,
                networkOperator,
                networkCountry,
                signalStrengthProfile,
                bypassRootArtifacts,
                bypassDebugger,
                bypassMockLocation,
                targetHookMaxMethods,
                serviceClipboardNull,
                serviceBluetoothDisabled,
                serviceNfcDisabled,
                minCadence,
                maxCadence,
                waveAmplitude
        );
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float normalizeBearing(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        float normalized = value % 360f;
        return normalized < 0f ? normalized + 360f : normalized;
    }

    @NonNull
    private static String sanitize(@Nullable String value, @NonNull String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
