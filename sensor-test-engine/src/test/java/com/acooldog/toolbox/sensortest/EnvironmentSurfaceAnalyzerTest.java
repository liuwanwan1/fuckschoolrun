package com.acooldog.toolbox.sensortest;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class EnvironmentSurfaceAnalyzerTest {
    @Test
    public void hiddenRootAndSignalVectorsBecomeFindings() {
        EnvironmentSurfaceReport report = new EnvironmentSurfaceAnalyzer().analyze(
                new EnvironmentSurfaceSnapshot(
                        true,
                        false,
                        true,
                        true,
                        true,
                        "41001",
                        "983221",
                        "02:11:22:33:44:55",
                        "lab-region"
                )
        );

        assertTrue(report.toJson().contains(EnvironmentSurfaceReport.WATERMARK));
        assertTrue(report.getFindings().size() >= 5);
    }
}
