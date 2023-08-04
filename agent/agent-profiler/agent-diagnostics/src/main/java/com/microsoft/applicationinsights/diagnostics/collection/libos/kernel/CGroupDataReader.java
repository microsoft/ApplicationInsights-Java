// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.kernel;

import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;

/** Reads CGroup data from the host OS */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public interface CGroupDataReader {

  long getKmemLimit() throws OperatingSystemInteractionException;

  long getMemoryLimit() throws OperatingSystemInteractionException;

  long getMemorySoftLimit() throws OperatingSystemInteractionException;

  long getCpuLimit() throws OperatingSystemInteractionException;

  long getCpuPeriod() throws OperatingSystemInteractionException;
}
