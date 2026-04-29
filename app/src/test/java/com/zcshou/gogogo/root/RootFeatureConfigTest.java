package com.acooldog.toolbox.root;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RootFeatureConfigTest {
    @Test
    public void defaults_useLsposedFramework() {
        RootFeatureConfig config = RootFeatureConfig.defaults();

        assertEquals(RootFeatureConfig.InjectionFramework.LSPOSED, config.getInjectionFramework());
    }

    @Test
    public void moduleFromId_resolvesDiagnosticModule() {
        assertEquals(
                RootDiagnosticModule.SENSOR_INJECTION,
                RootDiagnosticModule.fromId(RootFeature.SENSOR_EVENT_INJECTION.getConfigKey())
        );
    }
}
