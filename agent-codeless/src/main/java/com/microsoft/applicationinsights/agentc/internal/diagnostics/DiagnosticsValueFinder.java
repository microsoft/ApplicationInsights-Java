package com.microsoft.applicationinsights.agentc.internal.diagnostics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface DiagnosticsValueFinder {

    @Nonnull
    String getName();

    @Nullable
    String getValue();
}
