package com.acooldog.toolbox.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RootFeatureConfigTest {
    @Test
    public void defaults_useLsposedFramework() {
        RootFeatureConfig config = RootFeatureConfig.defaults();

        assertEquals(RootFeatureConfig.InjectionFramework.LSPOSED, config.getInjectionFramework());
    }

    @Test
    public void defaults_disableRootModeUntilExplicitlyEnabled() {
        RootFeatureConfig config = RootFeatureConfig.defaults();

        assertFalse(config.isRootModeEnabled());
    }

    @Test
    public void defaults_enableAllDiagnosticModules() {
        RootFeatureConfig config = RootFeatureConfig.defaults();

        for (RootFeature feature : RootFeature.values()) {
            assertTrue(config.isEnabled(feature));
        }
    }

    @Test
    public void rootModeEnabled_canBeToggledInConfig() {
        RootFeatureConfig config = RootFeatureConfig.defaults().withRootModeEnabled(true);

        assertTrue(config.isRootModeEnabled());
    }

    @Test
    public void moduleFromId_resolvesDiagnosticModule() {
        assertEquals(
                RootDiagnosticModule.SENSOR_INJECTION,
                RootDiagnosticModule.fromId(RootFeature.SENSOR_EVENT_INJECTION.getConfigKey())
        );
    }

    @Test
    public void compatibilityCatalog_includesInternalFuckRunProfiles() {
        assertEquals(5, RootDiagnosticCompatibilityCatalog.all().size());
        assertEquals(
                "com.huachenjie.shandong_school",
                RootDiagnosticCompatibilityCatalog.all().get(0).getPackageName()
        );
    }
}
