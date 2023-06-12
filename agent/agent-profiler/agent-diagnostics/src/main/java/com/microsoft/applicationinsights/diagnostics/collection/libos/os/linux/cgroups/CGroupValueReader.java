// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.TwoStepProcReader;
import java.io.File;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public abstract class CGroupValueReader extends TwoStepProcReader {
  private final BigIncrementalCounter usage = new BigIncrementalCounter();

  public CGroupValueReader(String fileName) {
    super(new File(fileName), true);
  }

  @Override
  protected void parseLine(String line) {
    long newUsage = Long.parseLong(line);
    usage.newValue(newUsage);
  }

  public BigIncrementalCounter getUsage() {
    return usage;
  }
}
