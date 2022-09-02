// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Parent class providing an overview of all memory management functions of the VM. */
public interface MemoryManagement {

  /** Returns all pools that comprise the JVMs memory. */
  Collection<MemoryPool> getPools();

  /** Returns a specific memory pool associated with the given name. */
  Optional<MemoryPool> getPool(String name);

  /** Returns all garbage collectors that manage the memory on this JVM. */
  Set<GarbageCollector> getCollectors();

  /** Returns the up time of the JVM. */
  long getUptime();

  /** Returns the memory management group that manages this JVM. */
  MemoryManagers getCollectorGroup();
}
