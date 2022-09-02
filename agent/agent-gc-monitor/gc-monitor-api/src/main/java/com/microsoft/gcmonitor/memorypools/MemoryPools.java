// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.gcmonitor.memorypools;

import com.google.errorprone.annotations.Immutable;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import java.lang.management.MemoryPoolMXBean;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/** Provides a list of all possible memory pools, and factories for instantiating them. */
public enum MemoryPools {
  CODE_CACHE(CodeCache.POOL_NAME, CodeCache::new),
  CODE_HEAP_NON_NMETHODS(CodeHeapNonNmethods.POOL_NAME, CodeHeapNonNmethods::new),
  CODE_HEAP_PROFILED_NMETHODS(CodeHeapProfiledNmethods.POOL_NAME, CodeHeapProfiledNmethods::new),
  CODE_HEAP_NON_PROFILED_NMETHODS(
      CodeHeapNonProfiledNmethods.POOL_NAME, CodeHeapNonProfiledNmethods::new),
  PS_EDEN_SPACE(PsEdenSpace.POOL_NAME, PsEdenSpace::new),
  PS_OLD_GEN(PsOldGen.POOL_NAME, PsOldGen::new),
  PS_PERM_GEN(PsPermGen.POOL_NAME, PsPermGen::new),
  PS_SURVIVOR_SPACE(PsSurvivorSpace.POOL_NAME, PsSurvivorSpace::new),
  CMS_OLD_GEN(CmsOldGen.POOL_NAME, CmsOldGen::new),
  CMS_PERM_GEN(CmsPermGen.POOL_NAME, CmsPermGen::new),
  PAR_EDEN_SPACE(ParEdenSpace.POOL_NAME, ParEdenSpace::new),
  PAR_SURVIVOR_SPACE(ParSurvivorSpace.POOL_NAME, ParSurvivorSpace::new),
  G1_EDEN(G1Eden.POOL_NAME, G1Eden::new),
  G1_EDEN_SPACE(G1EdenSpace.POOL_NAME, G1EdenSpace::new),
  G1_SURVIVOR(G1Survivor.POOL_NAME, G1Survivor::new),
  G1_SURVIVOR_SPACE(G1SurvivorSpace.POOL_NAME, G1SurvivorSpace::new),
  G1_OLD_GEN(G1OldGen.POOL_NAME, G1OldGen::new),
  G1_PERM_GEN(G1PermGen.POOL_NAME, G1PermGen::new),
  EDEN_SPACE(EdenSpace.POOL_NAME, EdenSpace::new),
  SURVIVORS_PACE(SurvivorSpace.POOL_NAME, SurvivorSpace::new),
  TENURED_GEN(TenuredGen.POOL_NAME, TenuredGen::new),
  PERM_GEN_SHARED_RW(PermGenSharedRw.POOL_NAME, PermGenSharedRw::new),
  PERM_GEN_SHARED_RO(PermGenSharedRo.POOL_NAME, PermGenSharedRo::new),
  PERM_GEN(PermGen.POOL_NAME, PermGen::new),
  METASPACE(Metaspace.POOL_NAME, Metaspace::new),
  COMPRESSED_CLASS_SPACE(CompressedClassSpace.POOL_NAME, CompressedClassSpace::new),
  SHENANDOAH(Shenandoah.POOL_NAME, Shenandoah::new),
  Z_HEAP(ZHeap.POOL_NAME, ZHeap::new);

  @Immutable
  interface MemoryPoolFactory extends Function<Set<GarbageCollector>, MemoryPool> {}

  private final String poolName;
  private final MemoryPoolFactory factory;

  MemoryPools(String poolName, MemoryPoolFactory factory) {
    this.poolName = poolName;
    this.factory = factory;
  }

  public static Optional<MemoryPools> findPoolFor(String poolName) {
    return Arrays.stream(MemoryPools.values())
        .filter(pool -> pool.poolName.equals(poolName))
        .findFirst();
  }

  public static MemoryPool getMemoryPool(
      MBeanServerConnection connection, ObjectName name, Set<GarbageCollector> collectors) {
    MemoryPoolMXBean dataSource = JMX.newMXBeanProxy(connection, name, MemoryPoolMXBean.class);
    String id = dataSource.getName();

    Set<GarbageCollector> managers =
        Arrays.stream(dataSource.getMemoryManagerNames())
            .flatMap(
                memoryName -> collectors.stream().filter(col -> col.getName().equals(memoryName)))
            .collect(Collectors.toSet());

    Optional<MemoryPools> pool = MemoryPools.findPoolFor(id);

    if (pool.isPresent()) {
      return pool.get().factory.apply(managers);
    } else {
      throw new IllegalArgumentException("Cannot find memory pool " + id);
    }
  }

  public static class CodeCache extends MemoryPool {
    public static final String POOL_NAME = "Code Cache";

    public CodeCache(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, false, false, false);
    }
  }

  public static class CodeHeapNonNmethods extends MemoryPool {
    public static final String POOL_NAME = "CodeHeap 'non-nmethods'";

    public CodeHeapNonNmethods(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, false, false, false);
    }
  }

  public static class CodeHeapProfiledNmethods extends MemoryPool {
    public static final String POOL_NAME = "CodeHeap 'profiled nmethods'";

    public CodeHeapProfiledNmethods(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, false, false, false);
    }
  }

  public static class CodeHeapNonProfiledNmethods extends MemoryPool {
    public static final String POOL_NAME = "CodeHeap 'non-profiled nmethods'";

    public CodeHeapNonProfiledNmethods(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, false, false, false);
    }
  }

  public static class PsEdenSpace extends MemoryPool {
    public static final String POOL_NAME = "PS Eden Space";

    public PsEdenSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class PsOldGen extends MemoryPool {
    public static final String POOL_NAME = "PS Old Gen";

    public PsOldGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, false);
    }
  }

  public static class PsPermGen extends MemoryPool {
    public static final String POOL_NAME = "PS Perm Gen";

    public PsPermGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class PsSurvivorSpace extends MemoryPool {
    public static final String POOL_NAME = "PS Survivor Space";

    public PsSurvivorSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class CmsOldGen extends MemoryPool {
    public static final String POOL_NAME = "CMS Old Gen";

    public CmsOldGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, false);
    }
  }

  public static class CmsPermGen extends MemoryPool {
    public static final String POOL_NAME = "CMS Perm Gen";

    public CmsPermGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class ParEdenSpace extends MemoryPool {
    public static final String POOL_NAME = "Par Eden Space";

    public ParEdenSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class ParSurvivorSpace extends MemoryPool {
    public static final String POOL_NAME = "Par Survivor Space";

    public ParSurvivorSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class G1Eden extends MemoryPool {
    public static final String POOL_NAME = "G1 Eden";

    public G1Eden(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class G1EdenSpace extends MemoryPool {
    public static final String POOL_NAME = "G1 Eden Space";

    public G1EdenSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class G1Survivor extends MemoryPool {
    public static final String POOL_NAME = "G1 Survivor";

    public G1Survivor(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class G1SurvivorSpace extends MemoryPool {
    public static final String POOL_NAME = "G1 Survivor Space";

    public G1SurvivorSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class G1OldGen extends MemoryPool {
    public static final String POOL_NAME = "G1 Old Gen";

    public G1OldGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, false);
    }
  }

  public static class G1PermGen extends MemoryPool {
    public static final String POOL_NAME = "G1 Perm Gen";

    public G1PermGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, false, false, false);
    }
  }

  public static class EdenSpace extends MemoryPool {
    public static final String POOL_NAME = "Eden Space";

    public EdenSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class SurvivorSpace extends MemoryPool {
    public static final String POOL_NAME = "Survivor Space";

    public SurvivorSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, true);
    }
  }

  public static class TenuredGen extends MemoryPool {
    public static final String POOL_NAME = "Tenured Gen";

    public TenuredGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, false);
    }
  }

  public static class PermGenSharedRw extends MemoryPool {
    public static final String POOL_NAME = "Perm Gen [shared-rw]";

    public PermGenSharedRw(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class PermGenSharedRo extends MemoryPool {
    public static final String POOL_NAME = "Perm Gen [shared-ro]";

    public PermGenSharedRo(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class PermGen extends MemoryPool {
    public static final String POOL_NAME = "Perm Gen";

    public PermGen(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class Metaspace extends MemoryPool {
    public static final String POOL_NAME = "Metaspace";

    public Metaspace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class CompressedClassSpace extends MemoryPool {
    public static final String POOL_NAME = "Compressed Class Space";

    public CompressedClassSpace(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, false, false);
    }
  }

  public static class Shenandoah extends MemoryPool {
    public static final String POOL_NAME = "Shenandoah";

    public Shenandoah(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, true);
    }
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  public static class ZHeap extends MemoryPool {
    public static final String POOL_NAME = "ZHeap";

    public ZHeap(Set<GarbageCollector> managers) {
      super(POOL_NAME, managers, true, true, true);
    }
  }
}
