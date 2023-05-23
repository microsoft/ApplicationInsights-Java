// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.process;

import com.microsoft.applicationinsights.diagnostics.collection.jvm.ProcessData;
import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;

@SuppressWarnings({"JavaLangClash"})
public abstract class Process extends ProcessData implements TwoStepUpdatable {

  protected boolean isJava;

  public Process(String name, int pid) {
    super(name, pid);
  }

  public boolean isJava() {
    return isJava;
  }

  public abstract ProcessIOStats getIoStats();

  public abstract ProcessCPUStats getCpuStats();
}
