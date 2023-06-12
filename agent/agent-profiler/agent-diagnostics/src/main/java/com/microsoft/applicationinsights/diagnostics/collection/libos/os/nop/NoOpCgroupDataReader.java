// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop;

import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class NoOpCgroupDataReader implements CGroupDataReader {

  @Override
  public long getKmemLimit() {
    return -1;
  }

  @Override
  public long getMemoryLimit() {
    return -1;
  }

  @Override
  public long getMemorySoftLimit() {
    return -1;
  }

  @Override
  public long getCpuLimit() {
    return -1;
  }

  @Override
  public long getCpuPeriod() {
    return -1;
  }
}
