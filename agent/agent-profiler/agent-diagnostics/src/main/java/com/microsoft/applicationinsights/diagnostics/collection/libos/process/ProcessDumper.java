// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.process;

import java.util.List;

public interface ProcessDumper {

  Iterable<Process> all(boolean includeSelf);

  Process thisProcess();

  void poll();

  void closeProcesses(List<Integer> exclusions);
}
