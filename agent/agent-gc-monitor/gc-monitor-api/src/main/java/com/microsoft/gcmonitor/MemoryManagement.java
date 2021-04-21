package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Parent class providing an overview of all memory management functions of the VM
 */
public interface MemoryManagement {

    /**
     * All pools that comprise the JVMs memory
     *
     * @return a copy of the list of all pools
     */
    Collection<MemoryPool> getPools();

    /**
     * Return a specific memory pool associated with the given name
     *
     * @param name The name of the memory pool
     */
    Optional<MemoryPool> getPool(String name);

    /**
     * Get all garbage collectors that manage the memory on this JVM
     *
     * @return a copy of the set of all collectors
     */
    Set<GarbageCollector> getCollectors();

    /**
     * The up time of the JVM
     */
    long getUptime();

    /**
     * The memory management group that manages this JVM
     */
    MemoryManagers getCollectorGroup();

}