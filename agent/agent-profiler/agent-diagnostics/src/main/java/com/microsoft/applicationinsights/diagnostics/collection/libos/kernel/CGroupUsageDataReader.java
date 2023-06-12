// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import java.io.Closeable;
import java.util.List;
import javax.annotation.Nullable;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface CGroupUsageDataReader extends TwoStepUpdatable, Closeable {
  @Nullable
  List<Double> getTelemetry();
}
