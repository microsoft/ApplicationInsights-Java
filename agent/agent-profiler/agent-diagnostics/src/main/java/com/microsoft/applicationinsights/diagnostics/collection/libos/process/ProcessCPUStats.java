// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.process;

import com.microsoft.applicationinsights.diagnostics.collection.libos.TwoStepUpdatable;
import java.io.Closeable;
import java.math.BigInteger;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface ProcessCPUStats extends Closeable, TwoStepUpdatable {

  BigInteger getUserTime();

  BigInteger getSystemTime();

  BigInteger getTotalTime();

  BigInteger getPriority();

  BigInteger getNice();

  BigInteger getNumThreads();

  BigInteger getVmSize();

  BigInteger getRss();

  BigInteger getSwapped();
}
