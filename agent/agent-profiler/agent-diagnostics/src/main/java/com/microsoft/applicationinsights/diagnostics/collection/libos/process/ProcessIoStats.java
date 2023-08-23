// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.process;

import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import java.io.Closeable;
import java.math.BigInteger;

public interface ProcessIoStats extends TwoStepUpdatable, Closeable {
  BigInteger getIoRead();

  BigInteger getIoWrite();

  BigInteger getDiskRead();

  BigInteger getDiskWrite();
}
