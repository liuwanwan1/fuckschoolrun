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
}
