package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.Locale;

public final class RootSignalStrengthProfile {
    static final String KEY_WIFI_RSSI_DBM = "wifiRssiDbm";
    static final String KEY_WIFI_JITTER_DBM = "wifiRssiJitterDbm";
    static final String KEY_CELL_DBM = "cellDbm";
    static final String KEY_CELL_JITTER_DBM = "cellJitterDbm";

    private static final int MIN_WIFI_RSSI_DBM = -100;
    private static final int MAX_WIFI_RSSI_DBM = -30;
    private static final int MIN_CELL_DBM = -125;
    private static final int MAX_CELL_DBM = -50;
    private static final int MAX_JITTER_DBM = 20;

    private final int wifiRssiDbm;
    private final int wifiJitterDbm;
    private final int cellDbm;
    private final int cellJitterDbm;

    public RootSignalStrengthProfile(
            int wifiRssiDbm,
            int wifiJitterDbm,
            int cellDbm,
            int cellJitterDbm
    ) {
        this.wifiRssiDbm = clamp(wifiRssiDbm, MIN_WIFI_RSSI_DBM, MAX_WIFI_RSSI_DBM);
        this.wifiJitterDbm = clamp(wifiJitterDbm, 0, MAX_JITTER_DBM);
        this.cellDbm = clamp(cellDbm, MIN_CELL_DBM, MAX_CELL_DBM);
        this.cellJitterDbm = clamp(cellJitterDbm, 0, MAX_JITTER_DBM);
    }

    @NonNull
    public static RootSignalStrengthProfile defaults() {
        return new RootSignalStrengthProfile(-55, 3, -85, 4);
    }

    @NonNull
    public static RootSignalStrengthProfile fromJson(
            @Nullable JSONObject object,
            @NonNull RootSignalStrengthProfile fallback
    ) {
        if (object == null) {
            return fallback;
        }
        return new RootSignalStrengthProfile(
                object.optInt(KEY_WIFI_RSSI_DBM, fallback.getWifiRssiDbm()),
                object.optInt(KEY_WIFI_JITTER_DBM, fallback.getWifiJitterDbm()),
                object.optInt(KEY_CELL_DBM, fallback.getCellDbm()),
                object.optInt(KEY_CELL_JITTER_DBM, fallback.getCellJitterDbm())
        );
    }

    public void writeToJson(@NonNull JSONObject object) {
        try {
            object.put(KEY_WIFI_RSSI_DBM, wifiRssiDbm);
            object.put(KEY_WIFI_JITTER_DBM, wifiJitterDbm);
            object.put(KEY_CELL_DBM, cellDbm);
            object.put(KEY_CELL_JITTER_DBM, cellJitterDbm);
        } catch (Exception ignored) {
            // Keep best-effort persistence aligned with RootDiagnosticSettings.
        }
    }

    public int getWifiRssiDbm() {
        return wifiRssiDbm;
    }

    public int getWifiJitterDbm() {
        return wifiJitterDbm;
    }

    public int getCellDbm() {
        return cellDbm;
    }

    public int getCellJitterDbm() {
        return cellJitterDbm;
    }

    public int getWifiLevel() {
        if (wifiRssiDbm >= -55) {
            return 4;
        }
        if (wifiRssiDbm >= -67) {
            return 3;
        }
        if (wifiRssiDbm >= -80) {
            return 2;
        }
        if (wifiRssiDbm >= -90) {
            return 1;
        }
        return 0;
    }

    public int getCellLevel() {
        if (cellDbm >= -85) {
            return 4;
        }
        if (cellDbm >= -95) {
            return 3;
        }
        if (cellDbm >= -105) {
            return 2;
        }
        if (cellDbm >= -115) {
            return 1;
        }
        return 0;
    }

    public int getCellAsuLevel() {
        return clamp((cellDbm + 113) / 2, 0, 31);
    }

    @NonNull
    public String summarize() {
        return String.format(
                Locale.getDefault(),
                "wifi=%ddBm±%d, cell=%ddBm±%d, wifiLevel=%d, cellLevel=%d",
                wifiRssiDbm,
                wifiJitterDbm,
                cellDbm,
                cellJitterDbm,
                getWifiLevel(),
                getCellLevel()
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
