// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfo;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfoReader;
import java.io.IOException;

public class NoOpMemoryInfoReader implements MemoryInfoReader {
  @Override
  public void poll() throws OperatingSystemInteractionException {}

  @Override
  public void update() throws OperatingSystemInteractionException {}

  @Override
  public MemoryInfo getMemoryInfo() {
    return new MemoryInfo(-1, -1, -1, -1);
  }

  @Override
  public void close() throws IOException {}
}
