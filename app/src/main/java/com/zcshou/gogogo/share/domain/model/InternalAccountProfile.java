package com.acooldog.toolbox.share.domain.model;

public final class InternalAccountProfile {
    private final String id;
    private final String username;
    private final String remark;
    private final String status;

    public InternalAccountProfile(String id, String username, String remark, String status) {
        this.id = id == null ? "" : id.trim();
        this.username = username == null ? "" : username.trim();
        this.remark = remark == null ? "" : remark.trim();
        this.status = status == null ? "" : status.trim();
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRemark() {
        return remark;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }
}
