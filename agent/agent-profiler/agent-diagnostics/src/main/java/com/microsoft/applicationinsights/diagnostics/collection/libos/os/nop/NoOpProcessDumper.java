// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop;

import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessDumper;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NoOpProcessDumper implements ProcessDumper, Closeable {

  @Override
  public void close() throws IOException {}

  @Override
  public Iterable<Process> all(boolean includeSelf) {
    return new ArrayList<>();
  }

  @Override
  public void poll() {}

  @Override
  public void closeProcesses(List<Integer> exclusions) {}

  @Override
  public Process thisProcess() {
    return null;
  }
}
