package com.acooldog.toolbox.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class RootSensorMotionProfileTest {
    @Test
    public void constructor_clampsJitterAndWaveformSamples() {
        RootSensorMotionProfile profile = new RootSensorMotionProfile(
                9d,
                2d,
                Arrays.asList(-2d, -1d, -0.5d, 0d, 0.5d, 1d, 2d, 0.25d)
        );

        assertEquals(3d, profile.getNaturalJitterRange(), 0.0001d);
        assertEquals(1d, profile.getNaturalJitterProbability(), 0.0001d);
        assertEquals(-1d, profile.getWaveformSamples().get(0), 0.0001d);
        assertEquals(1d, profile.getWaveformSamples().get(6), 0.0001d);
    }

    @Test
    public void normalizeRecordedMagnitudes_returnsEditableWaveform() {
        List<Double> waveform = RootSensorMotionProfile.normalizeRecordedMagnitudes(
                Arrays.asList(9.8d, 10.4d, 10.9d, 10.3d, 9.7d, 9.2d, 8.9d, 9.3d),
                16
        );

        assertEquals(16, waveform.size());
        for (double value : waveform) {
            assertTrue(value >= -1d && value <= 1d);
        }
    }

    @Test
    public void settingsWithSensor_preservesMotionProfile() {
        RootSensorMotionProfile profile = new RootSensorMotionProfile(
                0.75d,
                0.42d,
                Arrays.asList(-1d, -0.5d, 0d, 0.5d, 1d, 0.5d, 0d, -0.5d)
        );
        RootDiagnosticSettings settings = RootDiagnosticSettings.defaults().withSensor(150d, 170d, 4d, profile);

        assertEquals(0.75d, settings.getSensorNaturalJitterRange(), 0.0001d);
        assertEquals(0.42d, settings.getSensorNaturalJitterProbability(), 0.0001d);
        assertEquals(8, settings.getSensorMotionProfile().getWaveformSamples().size());
    }
}
