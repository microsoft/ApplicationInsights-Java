// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;

public interface KernelStatsReader {

  KernelCounters getCounters() throws OperatingSystemInteractionException;
}
