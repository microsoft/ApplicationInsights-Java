package com.microsoft.gcmonitor.memorypools;


import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;

import java.util.Collections;
import java.util.Set;

/**
 * Representation of a JVM memory pool
 */
public abstract class MemoryPool {

    private final String name;
    private final Set<GarbageCollector> garbageCollectors;
    private final boolean isTenuredPool;
    private final boolean isYoungPool;
    private final boolean isHeap;

    MemoryPool(String name, Set<GarbageCollector> garbageCollectors, boolean isHeap, boolean isTenuredPool, boolean isYoungPool) {
        this.name = name;
        this.garbageCollectors = Collections.unmodifiableSet(garbageCollectors);
        this.isHeap = isHeap;
        this.isTenuredPool = isTenuredPool;
        this.isYoungPool = isYoungPool;
    }

    /**
     * The name of the memory pool
     */
    public String getName() {
        return name;
    }

    /**
     * Determines if this memory pool is managed by the given collector
     */
    public boolean isManagedBy(GarbageCollector collector) {
        return garbageCollectors.contains(collector);
    }

    /**
     * If this collector manages the JVMs heap
     */
    public boolean isHeap() {
        return isHeap;
    }

    /**
     * If this is the JVMs tenured pool
     */
    public boolean isTenuredPool() {
        return isTenuredPool;
    }

    /**
     * If this pool is part of the young generation
     */
    public boolean isYoungPool() {
        return isYoungPool;
    }
}
