// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import com.microsoft.applicationinsights.diagnostics.collection.libos.net.GlobalNetworkStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.NetworkInterfaceStats;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Extracts global network statistics from /proc */
class LinuxGlobalNetworkStats extends TwoStepProcReader implements GlobalNetworkStats {

  private static final String NETSTAT_FILE_LOCATION = "/proc/net/dev";
  private static final Pattern DATA_LINE = Pattern.compile("^[A-Za-z0-9]+: .*");

  private static final int NAME_INDEX = 0;
  private static final int RECIEVED_INDEX = 1;
  private static final int TRANSMIT_INDEX = 9;

  private final Map<String, NetworkInterfaceStats> stats = new HashMap<>();

  public LinuxGlobalNetworkStats() {
    super(NETSTAT_FILE_LOCATION);
  }

  @Override
  protected void parseLine(String line) {

    String trimmed = line.trim();
    if (DATA_LINE.matcher(trimmed).matches()) {
      String[] data = line.split(" +");

      String interfaceName = data[NAME_INDEX].replace(":", "");

      long receivedBytes = Long.parseLong(data[RECIEVED_INDEX]);
      long sentBytes = Long.parseLong(data[TRANSMIT_INDEX]);

      updateInterface(interfaceName, receivedBytes, sentBytes);
    }
  }

  private void updateInterface(String interfaceName, long recievedBytes, long sentBytes) {
    NetworkInterfaceStats interf = stats.get(interfaceName);

    if (interf == null) {
      interf = new NetworkInterfaceStats(interfaceName);
      stats.put(interfaceName, interf);
    }
    interf.newReceivedValue(recievedBytes);
    interf.newSentValue(sentBytes);
  }

  @Override
  public BigInteger getTotalWrite() {
    BigInteger accumulator = BigInteger.ZERO;
    for (NetworkInterfaceStats stat : stats.values()) {
      BigInteger sent = stat.getSent();
      if (sent != null) {
        accumulator = accumulator.add(sent);
      }
    }

    return accumulator;
  }

  @Override
  public BigInteger getTotalReceived() {
    BigInteger accumulator = BigInteger.ZERO;
    for (NetworkInterfaceStats stat : stats.values()) {
      BigInteger received = stat.getReceived();
      if (received != null) {
        accumulator = accumulator.add(received);
      }
    }

    return accumulator;
  }
}
