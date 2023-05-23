// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class NoOpCgroupUsageDataReader implements CGroupUsageDataReader {
  @Override
  @Nullable
  public List<Double> getTelemetry() {
    return null;
  }

  @Override
  public void poll() {}

  @Override
  public void update() {}

  @Override
  public void close() throws IOException {}
}
