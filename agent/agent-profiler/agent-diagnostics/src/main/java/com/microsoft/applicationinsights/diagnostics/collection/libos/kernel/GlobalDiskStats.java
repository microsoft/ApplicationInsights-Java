// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

@SuppressWarnings({"checkstyle:AbbreviationAsWordInName"})
public interface GlobalDiskStats {

  long getTotalWrite();

  long getTotalRead();

  long getTotalIO();
}
