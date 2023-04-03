// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.net;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import java.math.BigInteger;

public class NetworkInterfaceStats {
  private final String name;
  private final BigIncrementalCounter bytesSent;
  private final BigIncrementalCounter bytesReceived;

  public NetworkInterfaceStats(String name) {
    this.name = name;
    this.bytesSent = new BigIncrementalCounter();
    this.bytesReceived = new BigIncrementalCounter();
  }

  public String getName() {
    return name;
  }

  public void newSentValue(long value) {
    bytesSent.newValue(BigInteger.valueOf(value));
  }

  public void newReceivedValue(long value) {
    bytesReceived.newValue(BigInteger.valueOf(value));
  }

  public BigInteger getSent() {
    return bytesSent.getIncrement();
  }

  public BigInteger getReceived() {
    return bytesReceived.getIncrement();
  }
}
