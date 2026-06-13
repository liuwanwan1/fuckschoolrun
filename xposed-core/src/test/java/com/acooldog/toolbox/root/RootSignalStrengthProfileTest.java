package com.acooldog.toolbox.root;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RootSignalStrengthProfileTest {
    @Test
    public void constructor_clampsSignalStrengthRange() {
        RootSignalStrengthProfile profile = new RootSignalStrengthProfile(-10, 99, -200, 99);

        assertEquals(-30, profile.getWifiRssiDbm());
        assertEquals(20, profile.getWifiJitterDbm());
        assertEquals(-125, profile.getCellDbm());
        assertEquals(20, profile.getCellJitterDbm());
    }

    @Test
    public void levels_followConfiguredDbm() {
        RootSignalStrengthProfile profile = new RootSignalStrengthProfile(-82, 0, -108, 0);

        assertEquals(1, profile.getWifiLevel());
        assertEquals(1, profile.getCellLevel());
        assertEquals(2, profile.getCellAsuLevel());
    }

    @Test
    public void settingsWithSignal_preservesSignalStrengthProfile() {
        RootDiagnosticSettings settings = RootDiagnosticSettings.defaults().withSignal(
                "02:00:00:11:22:33",
                "Internal-Lab",
                "46001",
                "cn",
                new RootSignalStrengthProfile(-70, 5, -101, 6)
        );

        assertEquals(-70, settings.getWifiRssiDbm());
        assertEquals(5, settings.getWifiJitterDbm());
        assertEquals(-101, settings.getCellDbm());
        assertEquals(6, settings.getCellJitterDbm());
    }

    @Test
    public void signalScenario_linksGpsWifiAndCellStrength() {
        RootDiagnosticSettings settings = RootDiagnosticSettings.defaults()
                .withSignalScenario(RootDiagnosticSettings.SignalScenario.WEAK);

        assertEquals(RootDiagnosticSettings.SignalScenario.WEAK, settings.resolveSignalScenario());
        assertEquals(3, settings.getLocationSatellites());
        assertEquals(5.5d, settings.getLocationHdop(), 0.0001d);
        assertEquals(-90, settings.getWifiRssiDbm());
        assertEquals(-116, settings.getCellDbm());
    }

    @Test
    public void automaticSignalIdentity_resetsClosedLabProfile() {
        RootDiagnosticSettings settings = RootDiagnosticSettings.defaults()
                .withSignal("02:00:00:11:22:33", "Internal-Lab", "46001", "us")
                .withAutomaticSignalIdentity();

        assertEquals("02:00:00:7a:11:29", settings.getWifiBssid());
        assertEquals("Internal-Test-WiFi", settings.getWifiSsid());
        assertEquals("46000", settings.getNetworkOperator());
        assertEquals("cn", settings.getNetworkCountry());
    }
}
