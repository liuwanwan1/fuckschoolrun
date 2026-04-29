package com.acooldog.toolbox.sensortest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SensorStressEngineTest {
    @Test
    public void constantCadenceReportContainsWatermarkAndSamples() {
        SensorStressReport report = new SensorStressEngine().generate(
                SensorStressConfig.create(SensorStressMode.CONSTANT_CADENCE, 180, 10, 7L)
        );

        assertEquals(200, report.getSamples().size());
        assertTrue(report.toJson().contains(SensorStressReport.WATERMARK));
        assertTrue(report.getTotalSteps() > 0);
        assertTrue(report.getFindings().size() >= 1);
    }

    @Test
    public void spikeModeMarksAnomalySamples() {
        SensorStressReport report = new SensorStressEngine().generate(
                SensorStressConfig.create(SensorStressMode.SPIKE_ANOMALY, 170, 30, 11L)
        );

        assertTrue(report.getAnomalySamples() > 0);
        assertTrue(report.getRiskScore() >= 40);
    }
}
