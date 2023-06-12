// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.net;

import java.math.BigInteger;

public interface GlobalNetworkStats {
  BigInteger getTotalWrite();

  BigInteger getTotalReceived();
}
