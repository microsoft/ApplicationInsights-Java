package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.microsoft.applicationinsights.internal.util.PropertyHelper;

public class SdkVersionFinder implements DiagnosticsValueFinder {
    @Nonnull
    @Override
    public String getName() {
        return "sdkVersion";
    }

    @Nullable
    @Override
    public String getValue() {
        return PropertyHelper.getSdkVersionNumber();
    }
}
