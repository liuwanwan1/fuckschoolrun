package com.acooldog.toolbox.root;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RootEnvironmentReport {
    private final List<String> rootManagerPackages;
    private final List<String> suBinaryPaths;
    private final List<String> hookFrameworkPackages;
    private final boolean developerOptionsEnabled;
    private final boolean mockLocationAllowedForThisApp;
    private final boolean legacyMockLocationEnabled;
    private final boolean hiddenRootLikely;
    private final long checkedAtMillis;

    public RootEnvironmentReport(
            @NonNull List<String> rootManagerPackages,
            @NonNull List<String> suBinaryPaths,
            @NonNull List<String> hookFrameworkPackages,
            boolean developerOptionsEnabled,
            boolean mockLocationAllowedForThisApp,
            boolean legacyMockLocationEnabled,
            boolean hiddenRootLikely,
            long checkedAtMillis
    ) {
        this.rootManagerPackages = copy(rootManagerPackages);
        this.suBinaryPaths = copy(suBinaryPaths);
        this.hookFrameworkPackages = copy(hookFrameworkPackages);
        this.developerOptionsEnabled = developerOptionsEnabled;
        this.mockLocationAllowedForThisApp = mockLocationAllowedForThisApp;
        this.legacyMockLocationEnabled = legacyMockLocationEnabled;
        this.hiddenRootLikely = hiddenRootLikely;
        this.checkedAtMillis = checkedAtMillis;
    }

    @NonNull
    public List<String> getRootManagerPackages() {
        return rootManagerPackages;
    }

    @NonNull
    public List<String> getSuBinaryPaths() {
        return suBinaryPaths;
    }

    @NonNull
    public List<String> getHookFrameworkPackages() {
        return hookFrameworkPackages;
    }

    public boolean isDeveloperOptionsEnabled() {
        return developerOptionsEnabled;
    }

    public boolean isMockLocationAllowedForThisApp() {
        return mockLocationAllowedForThisApp;
    }

    public boolean isLegacyMockLocationEnabled() {
        return legacyMockLocationEnabled;
    }

    public boolean isHiddenRootLikely() {
        return hiddenRootLikely;
    }

    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }

    public boolean hasRootIndicators() {
        return !rootManagerPackages.isEmpty() || !suBinaryPaths.isEmpty();
    }

    public boolean hasHookFrameworkIndicators() {
        return !hookFrameworkPackages.isEmpty();
    }

    public boolean hasLsposedManager() {
        return hookFrameworkPackages.contains(LsposedDiagnosticBridge.LSPOSED_MANAGER_PACKAGE);
    }

    @NonNull
    public String summarizeForAudit() {
        return "rootManagers=" + joinOrNone(rootManagerPackages)
                + ", suPaths=" + joinOrNone(suBinaryPaths)
                + ", hookFrameworks=" + joinOrNone(hookFrameworkPackages)
                + ", developerOptions=" + developerOptionsEnabled
                + ", mockLocationForApp=" + mockLocationAllowedForThisApp
                + ", legacyMockLocation=" + legacyMockLocationEnabled
                + ", hiddenRootLikely=" + hiddenRootLikely;
    }

    @NonNull
    private static List<String> copy(@NonNull List<String> source) {
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    @NonNull
    private static String joinOrNone(@NonNull List<String> values) {
        if (values.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append('|');
            }
            builder.append(values.get(index));
        }
        return builder.toString();
    }
}
