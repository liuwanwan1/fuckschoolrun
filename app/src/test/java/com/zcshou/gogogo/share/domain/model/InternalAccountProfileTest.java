package com.acooldog.toolbox.share.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertTrue(profile.canUseRootDiagnostics());
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
        assertTrue(profile.canUseRootDiagnostics());
    }

    @Test
    public void missingTesterTypeIsNotOrdinaryTester() {
        InternalAccountProfile profile = new InternalAccountProfile(
                "id",
                "tester",
                "",
                "",
                "",
                "active"
        );

        assertEquals("", profile.getTesterType());
        assertEquals("未分类账号", profile.getTesterTypeLabel());
        assertFalse(profile.hasTesterType());
        assertFalse(profile.canUseRootDiagnostics());
    }

    @Test
    public void inactiveOrdinaryTesterCannotUseRootDiagnostics() {
        InternalAccountProfile profile = new InternalAccountProfile(
                "id",
                "tester",
                "",
                "ordinary",
                "",
                "banned"
        );

        assertFalse(profile.canUseRootDiagnostics());
    }
}
