// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.cores;

public class RuntimeCoreCounter implements CoreCounter {

  private static final int AVAILABLE_CORES = Runtime.getRuntime().availableProcessors();

  @Override
  public int getCoreCount() {
    return AVAILABLE_CORES;
  }
}
