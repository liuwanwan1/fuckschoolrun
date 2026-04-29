package com.acooldog.toolbox.share.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InternalAccountProfileTest {
    @Test
    public void usesBackendTesterTypeLabel() {
        InternalAccountProfile profile = new InternalAccountProfile(
                "id",
                "tester",
                "",
                "donor",
                "贡献者账号",
                "active"
        );

        assertEquals("donor", profile.getTesterType());
        assertEquals("贡献者账号", profile.getTesterTypeLabel());
    }

    @Test
    public void mapsMissingTesterTypeLabel() {
        InternalAccountProfile profile = new InternalAccountProfile(
                "id",
                "tester",
                "",
                "advanced",
                "",
                "active"
        );

        assertEquals("高级测试账号", profile.getTesterTypeLabel());
    }
}
