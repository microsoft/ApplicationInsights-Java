// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.hardware;

import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import java.io.Closeable;

public interface MemoryInfoReader extends TwoStepUpdatable, Closeable {
  MemoryInfo getMemoryInfo();
}
