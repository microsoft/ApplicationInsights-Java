package com.microsoft.gcmonitor;

import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollectors;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import com.microsoft.gcmonitor.memorypools.MemoryPools;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Represents a group of memory managers that as a group manages the JVMs heap. Typically for most
 * collectors this would comprise of a Young generational manager and a Tenured generation manger.
 */
public enum MemoryManagers {

    ParallelScavenge(GarbageCollectors.PSMarkSweep.class, GarbageCollectors.PSScavenge.class, MemoryPools.PSOldGen.class,
            GarbageCollectors.PSMarkSweep.class, GarbageCollectors.PSScavenge.class),

    ConcurrentMarkSweep(GarbageCollectors.ConcurrentMarkSweep.class, GarbageCollectors.ParNew.class, MemoryPools.CMSOldGen.class,
            GarbageCollectors.ConcurrentMarkSweep.class, GarbageCollectors.ParNew.class),

    MarkSweep(GarbageCollectors.MarkSweepCompact.class, GarbageCollectors.Copy.class, MemoryPools.TenuredGen.class,
            GarbageCollectors.MarkSweepCompact.class, GarbageCollectors.Copy.class),

    G1(GarbageCollectors.G1OldGeneration.class, GarbageCollectors.G1YoungGeneration.class, MemoryPools.G1OldGen.class,
            GarbageCollectors.G1OldGeneration.class, GarbageCollectors.G1YoungGeneration.class),

    Shenandoah(GarbageCollectors.ShenandoahCycles.class, GarbageCollectors.ShenandoahCycles.class, MemoryPools.Shenandoah.class,
            GarbageCollectors.ShenandoahCycles.class, GarbageCollectors.ShenandoahPauses.class),

    ZGC(GarbageCollectors.ZGC.class, GarbageCollectors.ZGC.class, MemoryPools.ZHeap.class, GarbageCollectors.ZGC.class);


    private final Class<? extends GarbageCollector>[] managers;
    private final Class<? extends GarbageCollector> tenuredCollector;
    private final Class<? extends GarbageCollector> youngCollector;
    private final Class<? extends MemoryPool> oldGen;

    public static MemoryManagers of(MemoryManagement manager) {
        Set<GarbageCollector> collectorIdentifiers = manager.getCollectors();
        for (MemoryManagers group : values()) {
            if (group.isComposedOf(collectorIdentifiers)) {
                return group;
            }
        }
        throw new IllegalArgumentException(
                "Unable to find garbage collector group for the memory manager");
    }

    MemoryManagers(Class<? extends GarbageCollector> tenuredCollector,
                   Class<? extends GarbageCollector> youngCollector,
                   Class<? extends MemoryPool> oldGen,
                   Class<? extends GarbageCollector>... allCollectors) {

        this.tenuredCollector = tenuredCollector;
        this.youngCollector = youngCollector;
        this.oldGen = oldGen;
        this.managers = allCollectors;
    }

    private boolean isComposedOf(Set<GarbageCollector> collectors) {
        Set<Class<? extends GarbageCollector>> collectorClasses = collectors.stream()
                .map(GarbageCollector::getClass)
                .collect(Collectors.toSet());
        return collectorClasses.containsAll(Arrays.asList(managers));
    }
}
