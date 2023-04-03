// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.net;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class TCPStats {

  private final long receivedQueue;
  private final long transferredQueue;

  public TCPStats(long receivedQueue, long transferredQueue) {
    this.receivedQueue = receivedQueue;
    this.transferredQueue = transferredQueue;
  }

  public long getTotalReceivedQueuesSize() {
    return receivedQueue;
  }

  public long getTotalTransferredQueuesSize() {
    return transferredQueue;
  }
}
