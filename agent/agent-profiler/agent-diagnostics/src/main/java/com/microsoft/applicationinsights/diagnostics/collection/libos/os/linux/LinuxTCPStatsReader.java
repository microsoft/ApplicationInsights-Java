// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import static java.lang.Integer.parseInt;

import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStats;
import com.microsoft.applicationinsights.diagnostics.collection.libos.net.TCPStatsReader;
import java.io.File;
import javax.annotation.concurrent.NotThreadSafe;

/** Obtains per-process TCP statistics */
@NotThreadSafe
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LinuxTCPStatsReader extends TwoStepProcReader implements TCPStatsReader {

  private static final int RX_END = 54;
  private static final int RX_START = 47;

  private static final int TX_END = 45;
  private static final int TX_START = 38;

  private static final int AS_HEX = 16;

  private static final String PROC_FILE = "/proc/net/tcp";

  private TCPStats stats;

  private long transferredQueue = 0;
  private long receivedQueue = 0;

  public LinuxTCPStatsReader() {
    super(new File(PROC_FILE));
  }

  @Override
  protected void parseLine(String line) {
    try {
      if (line.contains("sl")) {
        return;
      }
      transferredQueue += parseInt(line.substring(TX_START, TX_END), AS_HEX);
      String received = line.substring(RX_START, RX_END);
      receivedQueue += parseInt(received, AS_HEX);
    } catch (RuntimeException e) {
      // ignore
    }
  }

  @Override
  public void update() {
    transferredQueue = 0;
    receivedQueue = 0;

    super.update();

    stats = new TCPStats(receivedQueue, transferredQueue);
  }

  @Override
  public TCPStats getTCPStats() {
    return stats;
  }

  @Override
  protected boolean trim() {
    return false;
  }
}
