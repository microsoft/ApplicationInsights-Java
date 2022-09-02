// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.garbagecollectors;

/** Statistics available for a GarbageCollector. */
public interface GarbageCollectorStats {

  /** The number of collections that have been performed. */
  long getCollectionCount();

  /** The amount of execution time this collector has accumulated. */
  long getCollectionTime();
}
