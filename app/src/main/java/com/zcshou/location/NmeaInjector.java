package com.acooldog.toolbox.location;

import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class NmeaInjector {
    public static final String EXTRA_NMEA = "nmea";
    public static final int DEFAULT_SATELLITE_COUNT = 7;

    private NmeaInjector() {
    }

    public static void attachGeneratedNmea(@NonNull Location location) {
        attachGeneratedNmea(location, DEFAULT_SATELLITE_COUNT);
    }

    public static void attachGeneratedNmea(@NonNull Location location, int satelliteCount) {
        String nmea = NmeaGenerator.generateStandardSentences(
                location.getLatitude(),
                location.getLongitude(),
                location.hasAltitude() ? location.getAltitude() : 0d,
                location.hasSpeed() ? location.getSpeed() : 0d,
                location.hasBearing() ? location.getBearing() : 0d,
                location.getTime() > 0L ? location.getTime() : System.currentTimeMillis(),
                satelliteCount
        );
        attachNmea(location, nmea);
        applyHighPrecisionMetadata(location);
    }

    public static void attachNmea(@NonNull Location location, @Nullable String nmea) {
        if (nmea == null || nmea.trim().isEmpty()) {
            return;
        }
        Bundle extras = location.getExtras() == null
                ? new Bundle()
                : new Bundle(location.getExtras());
        extras.putString(EXTRA_NMEA, nmea.trim());
        location.setExtras(extras);
    }

    private static void applyHighPrecisionMetadata(@NonNull Location location) {
        if (!location.hasAccuracy()) {
            location.setAccuracy(1f);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!location.hasVerticalAccuracy()) {
                location.setVerticalAccuracyMeters(1f);
            }
            if (!location.hasSpeedAccuracy()) {
                location.setSpeedAccuracyMetersPerSecond(0.1f);
            }
            if (!location.hasBearingAccuracy()) {
                location.setBearingAccuracyDegrees(1f);
            }
        }
    }
}
