// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.jfr;

import com.microsoft.applicationinsights.diagnostics.collection.SystemStatsReader;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.Calibration;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.Calibrator;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.CalibratorDefault;
import com.microsoft.applicationinsights.diagnostics.collection.calibration.ContextSwitchingRunner;
import com.microsoft.applicationinsights.diagnostics.collection.cores.RuntimeCoreCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.OperatingSystemInteractionException;
import com.microsoft.applicationinsights.diagnostics.collection.libos.hardware.MemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.CGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.kernel.KernelMonitorDeviceDriver;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.OperatingSystemDetector;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxKernelMonitor;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxMemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.LinuxProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups.LinuxCGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpCGroupDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpCGroupUsageDataReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpKernelMonitor;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpMemoryInfoReader;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.nop.NoOpProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.Process;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessDumper;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ThisPidSupplier;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes components mostly as singletons, mostly required to aid the migration away from being
 * part of a DI framework which managed lifecycles.
 */
@SuppressWarnings("Java8ApiChecker")
public class SystemStatsProvider {
  private static final Logger logger = LoggerFactory.getLogger(SystemStatsProvider.class);

  private static final AtomicBoolean initialised = new AtomicBoolean(false);

  private static final Map<Class<?>, AtomicReference<?>> singletons = new HashMap<>();

  private SystemStatsProvider() {}

  public static void init(int thisPid) {
    // Ensure we only initialize once
    if (initialised.compareAndSet(false, true)) {
      singletons.put(ThisPidSupplier.class, new AtomicReference<>((ThisPidSupplier) () -> thisPid));

      if (singletons.get(Calibrator.class) == null) {
        try {
          getCalibration();
          getMachineStats();
          getCGroupData();

          // Close until needed
          close();
        } catch (RuntimeException e) {
          logger.error("Failed to initialise Code Optimizer", e);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T getSingleton(Class<T> clazz) {
    return (T) singletons.get(clazz).get();
  }

  @SuppressWarnings("unchecked")
  private static <T> T getSingleton(Class<T> clazz, Supplier<T> supplier) {
    AtomicReference<T> current = (AtomicReference<T>) singletons.get(clazz);

    if (current == null) {
      current = new AtomicReference<>();
      singletons.put(clazz, current);
    }

    if (current.get() == null) {
      T instance = supplier.get();
      if (!current.compareAndSet(null, instance)) {
        // If we failed to create a Closeable resource, close it
        if (instance instanceof Closeable) {
          try {
            ((Closeable) instance).close();
          } catch (IOException e) {
            logger.error("Failed to close", e);
          }
        }
      }
    }

    return current.get();
  }

  @SuppressWarnings({"checkstyle:AbbreviationAsWordInName", "MemberName"})
  public static CGroupData getCGroupData() {
    return getSingleton(
        CGroupData.class,
        () -> {
          try {
            CGroupDataReader reader = buildCGroupDataReader();
            CGroupData data = new CGroupData();

            return data.setKmemLimit(reader.getKmemLimit())
                .setMemoryLimit(reader.getMemoryLimit())
                .setMemorySoftLimit(reader.getMemorySoftLimit())
                .setCpuLimit(reader.getCpuLimit())
                .setCpuPeriod(reader.getCpuPeriod());

          } catch (RuntimeException | OperatingSystemInteractionException e) {
            logger.warn("No CGroup data present");
            return null;
          }
        });
  }

  public static MachineStats getMachineStats() {
    return getSingleton(
        MachineStats.class,
        () ->
            new MachineStats()
                .setContextSwitchesPerMs(getCalibration().getContextSwitchingRate())
                .setCoreCount(new RuntimeCoreCounter().getCoreCount()));
  }

  private static Calibration getCalibration() {
    return getSingleton(
        Calibration.class,
        () -> {
          Calibrator calibrator =
              new CalibratorDefault(
                  new ContextSwitchingRunner(), getKernelMonitor(), getThisProcess());

          return calibrator.calibrate();
        });
  }

  private static Process getThisProcess() {
    return getSingleton(
        Process.class,
        () -> {
          ProcessDumper processDumper = getProcessDumper();
          if (processDumper == null) {
            return null;
          }
          processDumper.poll();

          Process thisProcess = processDumper.thisProcess();
          processDumper.closeProcesses(Collections.singletonList(thisProcess.getPid()));
          return thisProcess;
        });
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private static CGroupDataReader buildCGroupDataReader() {
    switch (OperatingSystemDetector.getOperatingSystem()) {
      case LINUX:
        return new LinuxCGroupDataReader();
      default:
        return new NoOpCGroupDataReader();
    }
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private static ProcessDumper getProcessDumper() {
    ThisPidSupplier pidSupplier = getSingleton(ThisPidSupplier.class);
    switch (OperatingSystemDetector.getOperatingSystem()) {
      case LINUX:
        return new LinuxProcessDumper(false, pidSupplier.get());
      default:
        return new NoOpProcessDumper();
    }
  }

  @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
  private static CGroupUsageDataReader buildCGroupUsageDataReader() {
    return getSingleton(
        CGroupUsageDataReader.class,
        () -> {
          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              return new LinuxCGroupUsageDataReader();
            default:
              return new NoOpCGroupUsageDataReader();
          }
        });
  }

  private static MemoryInfoReader buildMemoryInfoReader() {
    return getSingleton(
        MemoryInfoReader.class,
        () -> {
          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              return new LinuxMemoryInfoReader();
            default:
              return new NoOpMemoryInfoReader();
          }
        });
  }

  private static SystemStatsReader getSystemStatsReader() {
    return getSingleton(SystemStatsReader.class, SystemStatsProvider::buildSystemStatsReader);
  }

  private static SystemStatsReader buildSystemStatsReader() {
    SystemStatsReader ssr =
        new SystemStatsReader(
            getKernelMonitor(),
            buildCGroupUsageDataReader(),
            getThisProcess().getCpuStats(),
            getThisProcess().getIoStats(),
            buildMemoryInfoReader());
    try {
      ssr.readTelemetry();
      ssr.readTelemetry();
    } catch (OperatingSystemInteractionException | SystemStatsReader.ReaderClosedException e) {
      logger.warn("Failed to read telemetry", e);
    }
    return ssr;
  }

  private static KernelMonitorDeviceDriver getKernelMonitor() {
    return getSingleton(
        KernelMonitorDeviceDriver.class,
        () -> {
          switch (OperatingSystemDetector.getOperatingSystem()) {
            case LINUX:
              return new LinuxKernelMonitor();
            case WINDOWS:
            case SOLARIS:
            case MAC_OS:
            default:
              return new NoOpKernelMonitor();
          }
        });
  }

  public static SystemStatsReader getStatsReader(int thisPidSupplier) {
    init(thisPidSupplier);
    return getSystemStatsReader();
  }

  public static void close() {
    // Shutdown all closable singletons
    singletons.forEach(
        (clazz, supplier) -> {
          Object instance = supplier.get();
          if (instance instanceof Closeable) {
            try {
              ((Closeable) instance).close();
            } catch (IOException e) {
              logger.error("Failed to close reader", e);
            }

            singletons.get(clazz).set(null);
          }
        });
  }
}
