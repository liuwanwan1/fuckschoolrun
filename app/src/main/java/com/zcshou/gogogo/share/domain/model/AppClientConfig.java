package com.acooldog.toolbox.share.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AppClientConfig {
    public static final String ROOT_ACCESS_ANONYMOUS = "anonymous";

    private final String noticeTitle;
    private final String noticeMessage;
    private final String qqGroupNumber;
    private final String bilibiliText;
    private final String bilibiliUrl;
    private final List<String> rootAccessAllowedTesterTypes;

    public AppClientConfig(
            String noticeTitle,
            String noticeMessage,
            String qqGroupNumber,
            String bilibiliText,
            String bilibiliUrl
    ) {
        this(
                noticeTitle,
                noticeMessage,
                qqGroupNumber,
                bilibiliText,
                bilibiliUrl,
                null
        );
    }

    public AppClientConfig(
            String noticeTitle,
            String noticeMessage,
            String qqGroupNumber,
            String bilibiliText,
            String bilibiliUrl,
            List<String> rootAccessAllowedTesterTypes
    ) {
        this.noticeTitle = noticeTitle == null ? "" : noticeTitle.trim();
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage.trim();
        this.qqGroupNumber = qqGroupNumber == null ? "" : qqGroupNumber.trim();
        this.bilibiliText = bilibiliText == null ? "" : bilibiliText.trim();
        this.bilibiliUrl = bilibiliUrl == null ? "" : bilibiliUrl.trim();
        this.rootAccessAllowedTesterTypes = normalizeRootAccessTypes(rootAccessAllowedTesterTypes);
    }

    public static AppClientConfig defaults() {
        return new AppClientConfig("", "", "", "", "", defaultRootAccessTypes());
    }

    public String getNoticeTitle() {
        return noticeTitle;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public String getQqGroupNumber() {
        return qqGroupNumber;
    }

    public String getBilibiliText() {
        return bilibiliText;
    }

    public String getBilibiliUrl() {
        return bilibiliUrl;
    }

    public List<String> getRootAccessAllowedTesterTypes() {
        return rootAccessAllowedTesterTypes;
    }

    public boolean canUseRootDiagnostics(InternalAccountProfile profile, boolean tokenPresent) {
        if (!tokenPresent) {
            return rootAccessAllowedTesterTypes.contains(ROOT_ACCESS_ANONYMOUS);
        }
        if (profile == null || !profile.isActive()) {
            return false;
        }
        return rootAccessAllowedTesterTypes.contains(profile.getTesterType());
    }

    private static List<String> normalizeRootAccessTypes(List<String> values) {
        List<String> source = values == null ? defaultRootAccessTypes() : values;
        List<String> normalized = new ArrayList<>();
        for (String value : source) {
            String type = normalizeRootAccessType(value);
            if (!type.isEmpty() && !normalized.contains(type)) {
                normalized.add(type);
            }
        }
        return Collections.unmodifiableList(normalized);
    }

    private static List<String> defaultRootAccessTypes() {
        List<String> defaults = new ArrayList<>();
        defaults.add(InternalAccountProfile.TYPE_ORDINARY);
        defaults.add(InternalAccountProfile.TYPE_ADVANCED);
        defaults.add(InternalAccountProfile.TYPE_PIONEER);
        return defaults;
    }

    private static String normalizeRootAccessType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("unauthenticated".equals(normalized)
                || "guest".equals(normalized)
                || "not_logged_in".equals(normalized)
                || "not-logged-in".equals(normalized)) {
            return ROOT_ACCESS_ANONYMOUS;
        }
        switch (normalized) {
            case ROOT_ACCESS_ANONYMOUS:
            case InternalAccountProfile.TYPE_ORDINARY:
            case InternalAccountProfile.TYPE_ADVANCED:
            case InternalAccountProfile.TYPE_DONOR:
            case InternalAccountProfile.TYPE_PIONEER:
                return normalized;
            default:
                return "";
        }
    }
}
