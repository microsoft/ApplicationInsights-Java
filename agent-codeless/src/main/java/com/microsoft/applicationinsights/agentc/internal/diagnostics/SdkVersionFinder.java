package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import com.microsoft.applicationinsights.internal.util.PropertyHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
