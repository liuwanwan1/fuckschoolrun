package com.acooldog.toolbox.root;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class RootDiagnosticHookScriptBuilderTest {
    @Test
    public void build_containsTargetPackageGuardAndEnabledModule() {
        String script = new RootDiagnosticHookScriptBuilder().build(
                "diag-test",
                "com.example.target",
                Arrays.asList(
                        RootDiagnosticModule.LOCATION_NMEA,
                        RootDiagnosticModule.DETECTION_BYPASS
                )
        );

        assertTrue(script.contains("const TARGET_PACKAGE = \"com.example.target\";"));
        assertTrue(script.contains("currentPackage !== TARGET_PACKAGE"));
        assertTrue(script.contains("installLocationNmeaHooks"));
        assertTrue(script.contains("installDetectionBypassHooks"));
    }

    @Test
    public void build_doesNotInstallDisabledModules() {
        String script = new RootDiagnosticHookScriptBuilder().build(
                "diag-test",
                "com.example.target",
                Arrays.asList(RootDiagnosticModule.SENSOR_INJECTION)
        );

        assertTrue(script.contains("installSensorInjectionHooks"));
        assertFalse(script.contains("safeInstall(\"root_nmea_injection\""));
        assertFalse(script.contains("safeInstall(\"system_service_stream_log\""));
    }

    @Test
    public void build_sensorModuleUsesCadenceSettingsAndSineWave() {
        RootDiagnosticSettings settings = RootDiagnosticSettings.defaults().withSensor(160d, 168d, 4.2d);

        String script = new RootDiagnosticHookScriptBuilder().build(
                "diag-test",
                "com.example.target",
                Arrays.asList(RootDiagnosticModule.SENSOR_INJECTION),
                settings
        );

        assertTrue(script.contains("sensorMinCadence: 160.000000"));
        assertTrue(script.contains("sensorMaxCadence: 168.000000"));
        assertTrue(script.contains("sensorNaturalJitterRange"));
        assertTrue(script.contains("sensorWaveform"));
        assertTrue(script.contains("waveAt(phase)"));
        assertTrue(script.contains("SensorManager.registerListener"));
        assertTrue(script.contains("TYPE_STEP_DETECTOR") || script.contains("type === 18"));
    }
}
