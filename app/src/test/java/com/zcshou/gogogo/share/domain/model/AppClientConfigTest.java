package com.acooldog.toolbox.share.domain.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class AppClientConfigTest {
    @Test
    public void donorUsesBackendRootAccessPolicy() {
        InternalAccountProfile donor = new InternalAccountProfile(
                "id",
                "tester",
                "",
                "donor",
                "贡献者账号",
                "active"
        );

        assertFalse(AppClientConfig.defaults().canUseRootDiagnostics(donor, true));

        AppClientConfig config = new AppClientConfig(
                "",
                "",
                "",
                "",
                "",
                Arrays.asList("ordinary", "donor")
        );

        assertTrue(config.canUseRootDiagnostics(donor, true));
    }
}
