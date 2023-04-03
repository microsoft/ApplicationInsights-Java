// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import com.microsoft.applicationinsights.diagnostics.collection.libos.IncrementalCounter;

public class DiskStats {
  private final String name;
  private final IncrementalCounter writeTime;
  private final IncrementalCounter readTime;
  private final IncrementalCounter ioTime;

  public DiskStats(String name) {
    this.name = name;
    this.writeTime = new IncrementalCounter();
    this.readTime = new IncrementalCounter();
    this.ioTime = new IncrementalCounter();
  }

  public String getName() {
    return name;
  }

  public long getWriteTime() {
    return writeTime.getIncrement();
  }

  public long getReadTime() {
    return readTime.getIncrement();
  }

  public long getIoTime() {
    return ioTime.getIncrement();
  }

  public void newReadTime(long time) {
    readTime.newValue(time);
  }

  public void newWriteTime(long time) {
    writeTime.newValue(time);
  }

  public void newIoTime(long time) {
    ioTime.newValue(time);
  }
}
